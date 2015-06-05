package cz.muni.fi.pv239.playonceplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author m.cimbalek
 */
public class PlaylistHistoryService extends Service {

    private static final String PLAYLIST_PREFRENCES_NAME = "playlist-data";
    private static final String LAST_EXPORTED_MONTH = "last-exported-month";
    private static final String EXPORT_DAY = "export-day";
    private static final int DEFAULT_EXPORT_DAY = 1;
    private static final String PLAYLIST_SAVE_DIR = "/Playlist history";
    private static final String STORED_PLAYLIST_TMP = "last-month-playlist.lmp";
    private static final String TAG = "PlaylistHistoryService";


    private final IBinder playlistBind = new PlaylistHistoryBinder();


    private int lastExportedMonth;
    private int exportDay;

    private List<Song> playedSongs;

    /* ---- PUBLIC METHODS ---- */

    /**
     * Creates service and loads saved data.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences storedPlaylistSettings = getSharedPreferences(PLAYLIST_PREFRENCES_NAME, MODE_PRIVATE);
        lastExportedMonth = storedPlaylistSettings.getInt(LAST_EXPORTED_MONTH, getLastMonth());
        exportDay = storedPlaylistSettings.getInt(EXPORT_DAY, DEFAULT_EXPORT_DAY);

        //black magic
        playedSongs = loadPlayedSongsFromFile();

        //obvious
        checkExport();
    }

    /**
     * Had-to-implement method from superclass
     *
     * @param intent intent
     * @return IBinder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return playlistBind;
    }

    public List<Song> getPlayedSongs() {
        return playedSongs;
    }

    /**
     * Provides export of last month played songs list. Can be used to forced export.
     *
     * @return message if export was successfull.
     */
    public String doExportPlaylist() {
        String message;
        Writer writer;
        File playlistToStore = getPlaylistStorageDir(String.valueOf(getCurrentYear()) + "-" + String.valueOf(getCurrentMonth()) + ".phf");
        if (isExternalStorageWritable()) {
            try {
                writer = new BufferedWriter(new FileWriter(playlistToStore));
                for (Song toWrite : playedSongs) {
                    writer.append(toWrite.toString()).append("\n");
                }
                writer.close();
                this.lastExportedMonth = getCurrentMonth();
                playedSongs = new ArrayList<>();
                message = "Playlist for \"" + playlistToStore.getName() + "\" saved at \"" + playlistToStore.getPath() + "\".";
            } catch (IOException e) {
                Log.e("Exception", "Playlist file write failed: " + e.toString());
                message = "Saving playlist for \"" + playlistToStore.getName() + "\" failed.";
            }
            return message;
        } else return "External storage is not writeable.";
    }

    /**
     * Adds song to the list of played songs during last month period.
     * @param song Song-to-save.
     * @return True if song was saved, False otherwise (song is already saved in list).
     */
    public boolean addPlayedSong(Song song) {
        //first check if it shouldn't already benn exported
        checkExport();
        if (!isAlreadyPlayedThisMonth(song)) {
            playedSongs.add(song);
            return true;
        }
        return false;
    }

    /**
     * Runs on destroy of service, persists current settings and actual list of played songs.
     */
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences storedPlaylistSettings = getSharedPreferences(PLAYLIST_PREFRENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = storedPlaylistSettings.edit();
        editor.putInt(LAST_EXPORTED_MONTH, lastExportedMonth);
        editor.putInt(EXPORT_DAY, exportDay);
        editor.commit();

        //magic
        writePlayedSongsToFile();
    }

    //may really need debug

    /**
     * Checks if song was already played in last month period.
     * @param song Song-to-check.
     * @return True if song was already played, False otherwise.
     */
    @SuppressWarnings("Unchecked")
    public boolean isAlreadyPlayedThisMonth(Song song) {
        return playedSongs.contains(song);
    }

    /**
     * Checks if external storage is available to write.
     *
     * @return True if wirteable, False otherwise.
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Sets day of month on ehich should be playlist exported.
     *
     * @param day Export day to set.
     */
    public void setExportDay(int day) {
        if (day > 31) {
            exportDay = 31;
        }
        exportDay = day;
    }


    /* ---- SUPPORT PRIVATE METHODS ---- */

    /**
     * @return Number of current month.
     */
    private int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    /**
     * @return Number of last month.
     */
    private int getLastMonth() {
        int month = Calendar.getInstance().get(Calendar.MONTH) - 1;
        if (month == 0) {
            return 12;
        }
        return month;
    }

    /**
     * @return Number of month that has been before last month.
     */
    private int getMonthBeforeLast() {
        int month = Calendar.getInstance().get(Calendar.MONTH) - 2;
        if (month < 1) {
            return month + 12;
        }
        return month;
    }

    /**
     * @return Current day of month.
     */
    private int getCurrentDayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @return Current year.
     */
    private int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Checks if it's time to export last month playlist.
     *
     * @return Info if export was succesfull or not, null if it's not time to export yet.
     */
    private String checkExport() {
        if (((lastExportedMonth == getLastMonth()) && (exportDay >= getCurrentDayOfMonth())
                || (lastExportedMonth <= getMonthBeforeLast()))
                && playedSongs != null && !playedSongs.isEmpty()) {
            return doExportPlaylist();
        }
        return null;
    }

    /**
     * Prepares file for exporting playlist.
     *
     * @param playlistName name of file to which playlist should be exported.
     * @return file for exporting playlist.
     */
    private File getPlaylistStorageDir(String playlistName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC) + PLAYLIST_SAVE_DIR, playlistName);
        //if (!file.mkdirs()) {
        //    Log.e("Error", "Directory " + file.getPath() + " not created.");
        //} else {
            Log.d(TAG, "File: \"" + file.getName() + "\" succefully created in \"" + file.getPath() + "\".");
        //}
        return file;
    }

    /**
     * Persists last month playlist.
     */
    private void writePlayedSongsToFile() {
        String filename = STORED_PLAYLIST_TMP;
        FileOutputStream fos;
        ObjectOutputStream oos;

        try {
            fos = openFileOutput(filename, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(playedSongs);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads last month playlist from persisted file.
     * @return Last month playlist.
     */
    private List<Song> loadPlayedSongsFromFile() {
        List<Song> storedPlayedSongs = null;
        try {
            FileInputStream fis = openFileInput(STORED_PLAYLIST_TMP);
            ObjectInputStream ois = new ObjectInputStream(fis);
            storedPlayedSongs = (ArrayList<Song>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(storedPlayedSongs == null) {
            storedPlayedSongs = new ArrayList<Song>();
        }
        return storedPlayedSongs;
    }


    /* ---- INNER CLASSES ---- */

    /**
     * No f*cking idea
     */
    public class PlaylistHistoryBinder extends Binder {
        PlaylistHistoryService getService() {
            return PlaylistHistoryService.this;
        }
    }
}
