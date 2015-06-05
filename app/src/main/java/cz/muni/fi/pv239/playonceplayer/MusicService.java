//pass the list of songs into the Service class, playing from it using the MediaPlayer class
// and keeping track of the position of the current song using the songPosn instance variable

package cz.muni.fi.pv239.playonceplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Random;
/**
 * Created by jrumanov on 4/24/15.
 */

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {//AudioManager.OnAudioFocusChangeListener


    //media player
    private MediaPlayer player;
    private PlaylistHistoryService playlistHistoryService;
    //song list
    private List<Song> songs;
    //current position
    private int songPosn;

    //to complete binding process between Service and Activity
    private final IBinder musicBind = new MusicBinder();

    private String songTitle="";
    private static final int NOTIFY_ID=1;

    //for shuffling the songs
    private boolean shuffle=false;
    private Random rand;

    //shuffled playlist helper
    private List<Song> shuffledList;
    //id of a song which is to be played next in shuffledList
    private int shuffledSongId;

    //true, if checkbox is checked, false otherwise
    private boolean shuffled;
    private boolean historyBound;


    private Intent phsIntent;

    public String getSongTitle(){
        return songTitle;
    }

    //--------------------------lifecycle methods--------------------------------
    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();

        initMusicPlayer();

        rand=new Random();


        phsIntent = new Intent(this, PlaylistHistoryService.class);
        bindService(phsIntent, historyConnection, Context.BIND_AUTO_CREATE);

        //AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
        //        AudioManager.AUDIOFOCUS_GAIN);

        //if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // could not get audio focus.
       // }
    }

//    @Override
//    public void onStartCommand(){
//
//    }

    @Override
    public IBinder onBind(Intent intent) {

        return musicBind;
    }

    //release resources when the Service instance is unbound
    //This will execute when the user exits the app - it stops the service
    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
            player = null;
        }
        //stopForeground(true);
    }
    //-------------lifecycle methods END---------------------------


    //------------onCompletionListener mandatory method------------
    //what to do when playing of one particular song end
    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
            playlistHistoryService.addPlayedSong(songs.get(songPosn));
        }
    }
    //----------onCompletionListener mandatory method END----------


    //------------onErrorListener mandatory method------------
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v("MUSIC PLAYER", "Playback Error");
        mp.reset();
        return false;
    }
    //----------onErrorListener mandatory method END------------

    //------------onPreparedListener mandatory method------------
    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //take the user back to the activity class when tap to the notification
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Notification.Builder builder = new Notification.Builder(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }
    //----------onPreparedListener mandatory method END-----------


    //------------OnAudioFocusChangeListener mandatory method------------
    //@Override
    //public void onAudioFocusChange(int focusChange) {
    //    player.stop();
    //}
    //----------OnAudioFocusChangeListener mandatory method END----------


    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //there are onPrepared, onCompletion, and onError methods to respond to these events
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    //method to pass the list of songs from the Activity
    public void setList(List<Song> theSongs){

        songs=theSongs;
    }



    public void playSong(){
        //play a song
        //to be able to use it even if the user is playing subsequent songs
        player.reset();

        //get song
        Song playSong = songs.get(songPosn);

        //get song title for notification
        songTitle=playSong.getTitle();

        //get id
        long currSong = Long.parseLong(playSong.getID());
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        //setting this URI as the data source for the MediaPlayer instance
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        //prepare the player
        player.prepareAsync();
    }


    //set the current song
    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    //act on the control from the user in Activity
    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    //go to the previous song
    public void playPrev(){
        songPosn--;
        if(songPosn < 0) songPosn=songs.size()-1;
        playSong();
    }

    public void playNext(){
        songPosn++;
        if(songPosn>=songs.size()) songPosn=0;

        playSong();
    }



        //set the shuffle flag
        public void setShuffle(){
            //if(shuffle) {
            //   !//shuffle=false;

            // }
            //else {
            shuffle=true;
            shuffledList = songs;
            Collections.shuffle(shuffledList);

            this.setList(shuffledList);

            }




    //part of the interaction between the Activity and Service classes, for which we also need a Binder instance
    public class MusicBinder extends Binder {
        MusicService getService() {

            return MusicService.this;
        }
    }

    private ServiceConnection historyConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaylistHistoryService.PlaylistHistoryBinder binder = (PlaylistHistoryService.PlaylistHistoryBinder) service;
            //get service
            playlistHistoryService = binder.getService();
            historyBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            historyBound = false;
        }
    };
}

