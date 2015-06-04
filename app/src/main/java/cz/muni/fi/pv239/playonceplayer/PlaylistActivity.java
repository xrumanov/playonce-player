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

    //controller
    //private MusicController controller;

    //activity and playback pause flags
    //private boolean paused=false, playbackPaused=false;



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

        //setup controller
        //setController();
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

//    @Override
//    protected void onResume(){
//        super.onResume();
//        if(paused){
//            setController();
//            paused=false;
//        }
//    }

//    @Override
//    protected void onPause(){
//        super.onPause();
//        paused=true;
//    }
//
//    @Override
//    protected void onStop() {
//        controller.hide();
//        super.onStop();
//    }
//
//    @Override
//    protected void onDestroy() {
//        stopService(playIntent);
//        musicSrv=null;
//        super.onDestroy();
//    }
    //-------mandatory methods for Activity lifecycle END

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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


    //---------------implementation of MediaPlayerControl widget------------------
//    @Override
//    public boolean canPause() {
//        return true;
//    }
//
//    @Override
//    public boolean canSeekBackward() {
//        return true;
//    }
//
//    @Override
//    public boolean canSeekForward() {
//        return true;
//    }
//
//    @Override
//    public int getAudioSessionId() {
//        return 0;
//    }
//
//    @Override
//    public int getBufferPercentage() {
//        return 0;
//    }
//
//    @Override
//    public int getCurrentPosition() {
//        if(musicSrv!=null && musicBound && musicSrv.isPng())
//            return musicSrv.getPosn();
//        else return 0;
//    }
//
//    @Override
//    public int getDuration() {
//        if(musicSrv!=null && musicBound && musicSrv.isPng())
//            return musicSrv.getDur();
//        else return 0;
//    }
//
//    @Override
//    public boolean isPlaying() {
//        if(musicSrv!=null && musicBound)
//            return musicSrv.isPng();
//        return false;
//    }
//
//    @Override
//    public void pause() {
//        playbackPaused=true;
//        musicSrv.pausePlayer();
//    }
//
//    @Override
//    public void seekTo(int pos) {
//        musicSrv.seek(pos);
//    }
//
//    @Override
//    public void start() {
//        musicSrv.go();
//    }
    //---------------implementation of MediaPlayerControl widget END------------------


    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();

        Intent i = new Intent(PlaylistActivity.this ,MainActivity.class);
        int pos = Integer.parseInt(view.getTag().toString());
        String playingTitle = songList.get(pos).getTitle();
        i.putExtra("name", playingTitle);
        startActivity(i);

//        if(playbackPaused){
//            setController();
//            playbackPaused=false;
//        }
//        controller.show(0);
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



    //set the controller up
//    private void setController(){
//        controller = new MusicController(this);
//        //set previous and next button listeners
//        controller.setPrevNextListeners(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playNext();
//            }
//        }, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playPrev();
//            }
//        });
//        //set and show
//        controller.setMediaPlayer(this);
//        controller.setAnchorView(findViewById(R.id.song_list));
//        controller.setEnabled(true);
//    }


//    private void playNext(){
//        musicSrv.playNext();
//        if(playbackPaused){
//            setController();
//            playbackPaused=false;
//        }
//        controller.show(0);
//    }
//
//
//    private void playPrev(){
//        musicSrv.playPrev();
//        if(playbackPaused){
//            setController();
//            playbackPaused=false;
//        }
//        controller.show(0);
//    }

    public void onCheckBoxShuffledClicked(View view){
        CheckBox checkbox = (CheckBox) view;
        if(checkbox.isChecked()){
            shuffled = true;
            shuffledList = songList;
            Collections.shuffle(shuffledList);
            String FYI_shuffle = "";
            for (int j = 0; j < shuffledList.size(); j++){
                FYI_shuffle += shuffledList.get(j).getArtist().toString()+"\n";
            }
            shuffledSongId=0;

            TextView tv = (TextView)findViewById(R.id.textView3);
            tv.setText("Shuffled arraylist is: \n"+FYI_shuffle);
            musicSrv.setList(shuffledList);
        }
        else {

            shuffled = false;
            TextView tv = (TextView)findViewById(R.id.textView3);
            tv.setText("Shuffle disabled!");
            musicSrv.setList(songList);
        }

    }

    public void onPlayClicked(View view){
        TextView tv = (TextView)findViewById(R.id.textView3);
        tv.setText("Play was just clicked!");

        if(shuffled){
             if (shuffledSongId == (songList.size()-1)){
                 tv.setText("You have reached the end of shuffled playlist");
             }
            else{
                 Song nextSong = songList.get(shuffledSongId);
                 tv.setText("Next song to play is " +nextSong.toString());
             }
        }
        else {
                //playNext();
                tv.setText("Next song in normal order.");

        }

    }

    public void onStopClicked(View view){
        TextView tv = (TextView)findViewById(R.id.textView3);
        tv.setText("Stop was just clicked!");

    }

    public void onBackClicked(View view){
        this.onDestroy();

    }

//    @Override
//    protected void onSaveInstanceState(Bundle bundle){
//        super.onSaveInstanceState(bundle);
    //never use it to store persistent data, only transient state of the activity - state of UI
    //you can test the state recreation by rotating the screen
//    }

}