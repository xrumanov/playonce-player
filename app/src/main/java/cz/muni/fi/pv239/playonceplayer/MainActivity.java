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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;

import cz.muni.fi.pv239.playonceplayer.MusicService.MusicBinder;

//MediaController presents a widget with play/pause, rewind, fast-forward, and skip (previous/next) buttons



public class MainActivity extends ActionBarActivity implements MediaPlayerControl {

   //service
    private MusicService musicSrv;
    private StreamService streamSrv;
    private Intent playIntent;
    private Intent phsIntent;
    private PlaylistHistoryService playlistHistoryService;
    //controller
    private MusicController controller;
    private ActionBar actionBar;


    //binding
    private boolean musicBound = false;
    private boolean historyBound = false;


    //activity and playback pause flags
    private boolean paused = false, playbackPaused = false;

    private Handler handler;

    //-------mandatory methods for Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //create ui
        setContentView(R.layout.activity_main);


        actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);


        if (getIntent().getStringExtra("name") != null) {
            actionBar.setTitle(getIntent().getStringExtra("name"));
        } else {
            actionBar.setTitle("Choose a song!");
        }
            setController();
        }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(getIntent().getStringExtra("name") != null){
            controller.show();
        }
    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            //bind service using musicConnection object
            phsIntent = new Intent(this, PlaylistHistoryService.class);
            bindService(phsIntent, historyConnection, Context.BIND_AUTO_CREATE);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(playIntent);
            startService(phsIntent);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
            setController();
        }
    }

    @Override
    protected void onPause() {
        paused = true;
        super.onPause();
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
        super.onDestroy();
    }
    //-------mandatory methods for Activity lifecycle END


    //deliver the IBinder that the client can use to communicate with the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            musicBound = false;
        }
    };

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
        if(musicSrv!=null && musicBound && musicSrv.isPng())//
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
        if(musicSrv!=null && musicBound) {
            return musicSrv.isPng();
        }
            playbackPaused = false;
        return false;
    }

    @Override
    public void pause() {
        playbackPaused =true;
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
                this.showStreams();
                break;
            case R.id.action_generated_playlists:

                break;
            case R.id.action_playlist_history:
                this.showPlaylistHistory();
                break;

        }
        return super.onOptionsItemSelected(item);

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
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }


    private void playNext(){
        musicSrv.playNext();
        actionBar.setTitle(musicSrv.getSongTitle());
        if(playbackPaused){
            playbackPaused=false;
            setController();
        }

        controller.show(0);
    }


    private void playPrev() {
        musicSrv.playPrev();
        actionBar.setTitle(musicSrv.getSongTitle());
        if (playbackPaused) {
            playbackPaused = false;
            setController();
        }
        controller.show(0);
    }


    //-------------auxiliary methods for menu------------------------

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

    private void showPlaylistHistory(){

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(MainActivity.this, PlaylistHistoryActivity.class);
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
}