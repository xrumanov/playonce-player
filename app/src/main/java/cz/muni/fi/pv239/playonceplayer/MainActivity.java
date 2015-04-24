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

import cz.muni.fi.pv239.playonceplayer.MusicService.MusicBinder;


public class MainActivity extends Activity implements MediaPlayerControl {

    //where songs are stored
    private ArrayList<Song> songList;
    //display the songs
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;

    //activity and playback pause flags
    private boolean paused=false, playbackPaused=false;

    //the widget
    private MusicController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //retrieve list view instance using the ID we gave it in the main layout
        songView = (ListView)findViewById(R.id.song_list);

        //instantiate list
        songList = new ArrayList<Song>();

        //get songs from device - helper method (see below)
        getSongList();

        //sort the songs alphabetically by title
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        //setup controller
        setController();


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

    //When the Activity instance starts, we create the Intent object, bind to it, and start it
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    //We set the song position as the tag for each item in the list view when we defined
    //the Adapter class. We retrieve it here and pass it to the Service instance
    public void songPicked(View view){
        Integer songIndex = (Integer) Integer.parseInt(view.getTag().toString());
        musicSrv.setSong(songIndex);
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

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
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //helper method to retrieve the audio file information
    public void getSongList(){

        //ContentResolver instance retrieves the URI for external music files
        ContentResolver musicResolver = getContentResolver();

        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //Cursor instance using the ContentResolver instance to query the music files
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        //iterate over results if valid
        //First retrieve the column indexes for the data items that we are interested in for each song,
        //then we use these to create a new Song object and add it to the list, before continuing
        //to loop through the results.
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public boolean canPause() {

        return true;
    }

    @Override
    public boolean canSeekBackward() {

        return true;
    }

    @Override
    public boolean canSeekForward() {

        return true;
    }

    @Override
    public int getAudioSessionId() {

        return 0;
    }

    @Override
    public int getBufferPercentage() {

        return 0;
    }

    //The conditional tests are to avoid various exceptions
    //that may occur when using the MediaPlayer and MediaController classes
    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }


    //set the controller up
    private void setController(){
        controller = new MusicController(this);

        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }

        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        //set the controller to work on media playback in the app, with its anchor view
        // referring to the list we included in the layout
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    //ensure that the controller displays when the user returns to the app
    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

}
