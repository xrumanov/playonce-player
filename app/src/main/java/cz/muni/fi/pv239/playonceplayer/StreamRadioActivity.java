package cz.muni.fi.pv239.playonceplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import cz.muni.fi.pv239.playonceplayer.StreamService.StreamBinder;


public class StreamRadioActivity extends ActionBarActivity {

    private Handler handler;
    private ArrayList<Radio> data;

    private StreamService streamSrv;
    private Intent streamIntent;
    private boolean streamBound = false;
    private boolean paused = false;


    //-------mandatory methods for Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_radio);

        data = new ArrayList<Radio>();
        data.add( new Radio(1, "Radio Slovensko","http://icecast.stv.livebox.sk/slovensko_128.mp3"));
        data.add( new Radio(2, "Europa2 SK","http://pool.cdn.lagardere.cz/fm-europa2sk-128"));
        data.add( new Radio(3, "Evropa2 CZ","http://icecast3.play.cz:80/evropa2-128.mp3"));


        ListView lv = (ListView) findViewById(R.id.radio_list);
        lv.setAdapter(new ArrayAdapter<Radio>(this, R.layout.radio_list, R.id.radioname, data));

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView< ?> parent, View view, final int position, long id) {


                streamSrv.setStream(data.get(position).getUri());
                streamSrv.playStream();
            }
    });
    }

    protected void onStart(){
        super.onStart();
        if(streamIntent == null){
            streamIntent = new Intent(StreamRadioActivity.this, StreamService.class);
            //bind service using musicConnection object
            bindService(streamIntent, streamConnection, Context.BIND_AUTO_CREATE);
            //resulting to onStartCommand method in service class
            startService(streamIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onStop() {
        //controller.hide();
        super.onStop();
        if (streamBound) {
            unbindService(streamConnection);
            streamBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        stopService(streamIntent);
        super.onDestroy();
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
//                musicSrv.setShuffle();
                break;
            case R.id.action_playlist:
                this.showPlaylist();

                break;
            case R.id.action_stream:
                break;
            case R.id.action_generated_playlists:
                break;

        }
        return super.onOptionsItemSelected(item);

    }

//deliver the stream IBinder that the client can use to communicate with the service
private ServiceConnection streamConnection = new ServiceConnection(){

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        StreamService.StreamBinder binder = (StreamBinder)service;
        //get service
        streamSrv = binder.getService();
        //pass list
        streamBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        streamBound = false;
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
                        Intent i = new Intent(StreamRadioActivity.this, PlaylistActivity.class);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        StreamRadioActivity.this.startActivity(i);
                    }
                });

            }
        };

        new Thread(runnable).start();
    }
}

//TODo pridat playlisty, pridat shuffle