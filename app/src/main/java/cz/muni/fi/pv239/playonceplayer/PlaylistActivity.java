//using the ContentResolver class to retrieve tracks on the device,
// the MediaPlayer class to play audio and the MediaController class to control playback
//use a Service instance to play audio when the user is not directly interacting with the app
//use an Adapter instance to present the songs in a list view,
// starting playback when the user taps an item from the list
//use the MediaController class to give the user control over playback,
//implement functions to skip forward and back, and include a shuffle function
//After this series, we will explore other aspects of media playback that can enhance the app,
// such as handling audio focus, presenting media files in different ways, and playing streaming media

package cz.muni.fi.pv239.playonceplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import android.app.Activity;
//MediaController presents a widget with play/pause, rewind, fast-forward, and skip (previous/next) buttons
import android.widget.MediaController.MediaPlayerControl;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import cz.muni.fi.pv239.playonceplayer.MusicService.MusicBinder;


public class PlaylistActivity extends ActionBarActivity {

    //song list variables
    private ArrayList<Song> songList;
    private ListView songView;

    //shuffled playlist helper
    private ArrayList<Song> shuffledList;
    //id of a song which is to be played next in shuffledList
    private int shuffledSongId;

    //true, if checkbox is checked, false otherwise
    private boolean shuffled;

    //service
    private MusicService musicSrv;
    private Intent playIntent;
    //binding
    private boolean musicBound=false;

    //-------mandatory methods for Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //create ui
        setContentView(R.layout.activity_playlist);

        //retrieve list view
        songView = (ListView) findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        getSongList();
        //sort alphabetically by title
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(playIntent);
        }
    }
    //-------mandatory methods for Activity lifecycle END

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                break;
            case R.id.action_playlist:
                break;
            case R.id.action_stream:
                if(musicSrv != null){
                    musicSrv.onDestroy();

                    //mozno este odbindovat a vsetko ukoncit
                }
                this.showStreams();
                break;
            case R.id.action_generated_playlists:

                break;

        }
        return super.onOptionsItemSelected(item);

    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();

        Intent i = new Intent(PlaylistActivity.this ,MainActivity.class);
        int pos = Integer.parseInt(view.getTag().toString());
        String playingTitle = songList.get(pos).getTitle();
        i.putExtra("name", playingTitle);
        startActivity(i);
    }

    //method to retrieve song info from device
    public void getSongList(){
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if (!musicCursor.moveToFirst()) {
            String thisId = "1";
            String thisTitle = "Streamy";
            String thisArtist = "Mc Kubo";
            songList.add(new Song(thisId, thisTitle, thisArtist));
            String thisTitle2 = "Just play";
            String thisArtist2 = "Mc Janca";
            songList.add(new Song("2", thisTitle2, thisArtist2));
            String thisTitle3 = "Connect";
            String thisArtist3 = "Mc Martin";
            songList.add(new Song("3", thisTitle3, thisArtist3));


        } else {
            if (musicCursor != null && musicCursor.moveToFirst()) {
                //get columns
                int titleColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.ARTIST);
                //add songs to list
                do {
                    String thisId = String.valueOf(musicCursor.getLong(idColumn));
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    songList.add(new Song(thisId, thisTitle, thisArtist));
                }
                while (musicCursor.moveToNext());
            }
        }
    }

    private void showStreams(){
        Intent i = new Intent(PlaylistActivity.this, StreamRadioActivity.class);
        PlaylistActivity.this.startActivity(i);
    }
}