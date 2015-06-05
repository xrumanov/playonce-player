package cz.muni.fi.pv239.playonceplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;


public class PlaylistHistoryActivity extends ActionBarActivity {

    private Handler handler;
    private List<Song> songList;

    private Intent playIntent;
    private ListView songView;
    private PlaylistHistoryService service;
    private MusicService musicSrv;
    private boolean musicBound = false;

    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder iService) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iService;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        songView = (ListView) findViewById(R.id.song_list);
        SongAdapter songAdt = new SongAdapter(this,songList);
        songView.setAdapter(songAdt);
        service = new PlaylistHistoryService();
        service.startService(playIntent);
        songList = service.getPlayedSongs();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist_history, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, PlaylistHistoryService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(playIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                break;
            case R.id.action_playlist:
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
            /*FIXME: case R.id.action_playlist_history:
                break;*/

        }
        return super.onOptionsItemSelected(item);

    }

    private void showStreams() {
        Intent i = new Intent(PlaylistHistoryActivity.this, StreamRadioActivity.class);
        PlaylistHistoryActivity.this.startActivity(i);
    }

    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();

        Intent i = new Intent(PlaylistHistoryActivity.this ,MainActivity.class);
        int pos = Integer.parseInt(view.getTag().toString());
        String playingTitle = songList.get(pos).getTitle();
        i.putExtra("name", playingTitle);
        startActivity(i);
    }

    private void showPlaylist(){

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(PlaylistHistoryActivity.this, PlaylistActivity.class);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        PlaylistHistoryActivity.this.startActivity(i);
                    }
                });

            }
        };

        new Thread(runnable).start();
    }
}
