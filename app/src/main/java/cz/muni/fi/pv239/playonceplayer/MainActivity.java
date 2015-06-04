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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController.MediaPlayerControl;

import cz.muni.fi.pv239.playonceplayer.MusicService.MusicBinder;
import cz.muni.fi.pv239.playonceplayer.StreamService.StreamBinder;

//MediaController presents a widget with play/pause, rewind, fast-forward, and skip (previous/next) buttons


public class MainActivity extends ActionBarActivity implements MediaPlayerControl {

    //    //song list variables
//    private ArrayList<Song> songList;
//    private ListView songView;
//
//    //service
    private MusicService musicSrv;
    private StreamService streamSrv;
    private Intent playIntent;
    private Intent streamIntent;
    //controller
    private MusicController controller;


    //binding
    private boolean musicBound = false;
    private boolean streamBound = false;

    //activity and playback pause flags
    private boolean paused = false, playbackPaused = false;

    //media player for internet radio streaming
    private MediaPlayer player = new MediaPlayer();
    private Handler handler;

    //-------mandatory methods for Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //create ui
        setContentView(R.layout.activity_main);


        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);


        if (getIntent().getStringExtra("name") != null) {
            actionBar.setTitle(getIntent().getStringExtra("name"));
            //controller.show(0);
        } else actionBar.setTitle("Choose a song!");
//        //retrieve list view
//        songView = (ListView)findViewById(R.id.song_list);
//        //instantiate list
//        songList = new ArrayList<Song>();
//        //get songs from device
//        getSongList();
//        //sort alphabetically by title
//        Collections.sort(songList, new Comparator<Song>() {
//            public int compare(Song a, Song b) {
//                return a.getTitle().compareTo(b.getTitle());
//            }
//        });
//        //create and set adapter
//        SongAdapter songAdt = new SongAdapter(this, songList);
//        songView.setAdapter(songAdt);
//
//        //setup controller

        if (isPlaying() || paused) {
            controller.show(0);
        } else {
            setController();
        }
    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            //bind service using musicConnection object
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(playIntent);

        }
    }

    public void buttonPlaylist(View view) {
        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(MainActivity.this, PlaylistActivity.class);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        MainActivity.this.startActivity(i);
                    }
                });

            }
        };

        new Thread(runnable).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unbindService(musicConnection);
        paused = true;
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        //musicSrv=null;
        player.stop();
        super.onDestroy();
    }
    //-------mandatory methods for Activity lifecycle END

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //deliver the IBinder that the client can use to communicate with the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            //musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            musicBound = false;
        }
    };

    //---------------implementation of MediaPlayerControl widget------------------
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
    //---------------implementation of MediaPlayerControl widget END------------------


    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //method to retrieve song info from device
//    public void getSongList(){
//        //query external audio
//        ContentResolver musicResolver = getContentResolver();
//        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
//        //iterate over results if valid
//        if (!musicCursor.moveToFirst()) {
//            long thisId = 1;
//            String thisTitle = "there is nothing to show";
//            String thisArtist = "";
//            songList.add(new Song(thisId, thisTitle, thisArtist));
//
//
//        } else {
//            if (musicCursor != null && musicCursor.moveToFirst()) {
//                //get columns
//                int titleColumn = musicCursor.getColumnIndex
//                        (android.provider.MediaStore.Audio.Media.TITLE);
//                int idColumn = musicCursor.getColumnIndex
//                        (android.provider.MediaStore.Audio.Media._ID);
//                int artistColumn = musicCursor.getColumnIndex
//                        (android.provider.MediaStore.Audio.Media.ARTIST);
//                //add songs to list
//                do {
//                    long thisId = musicCursor.getLong(idColumn);
//                    String thisTitle = musicCursor.getString(titleColumn);
//                    String thisArtist = musicCursor.getString(artistColumn);
//                    songList.add(new Song(thisId, thisTitle, thisArtist));
//                }
//                while (musicCursor.moveToNext());
//            }
//        }
//    }



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
        //set and show
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

//    @Override
//    protected void onSaveInstanceState(Bundle bundle){
//        super.onSaveInstanceState(bundle);
    //never use it to store persistent data, only transient state of the activity - state of UI
    //you can test the state recreation by rotating the screen
//    }



    //deliver the stream IBinder that the client can use to communicate with the service
    private ServiceConnection streamConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StreamBinder binder = (StreamBinder)service;
            //get service
            streamSrv = binder.getService();
            //pass list
            streamBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            musicBound = false;
        }
    };

    //show playlist activity
    private void showPlaylist(){

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(MainActivity.this, PlaylistActivity.class);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        MainActivity.this.startActivity(i);
                    }
                });

            }
        };

        new Thread(runnable).start();
    }


    private void showStreams(){
    Intent i = new Intent(MainActivity.this, StreamRadioActivity.class);
        MainActivity.this.startActivity(i);
    }

    //start stream radio
    private void startStream() {


        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        };

        if (streamIntent == null) {
            streamIntent = new Intent(this, StreamService.class);
            //bind service using streamConnection object
            bindService(streamIntent, streamConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(streamIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
//                musicSrv.setShuffle();
                break;
            case R.id.action_playlist:
                if(streamSrv != null){
                    streamSrv.onDestroy();
                }
                this.showPlaylist();

                break;
            case R.id.action_stream:
                if(musicSrv != null){
                    musicSrv.onDestroy();
                    //mozno este odbindovat a vsetko ukoncit
                }
                //this.startStream();
                this.showStreams();
                break;
            case R.id.action_generated_playlists:

                break;

        }
        return super.onOptionsItemSelected(item);

    }
}