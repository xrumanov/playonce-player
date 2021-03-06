//pass the list of songs into the Service class, playing from it using the MediaPlayer class
// and keeping track of the position of the current song using the songPosn instance variable

package cz.muni.fi.pv239.playonceplayer;

import android.app.Service;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.IBinder;
import android.content.Intent;

import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

/**
 * Created by jrumanov on 4/24/15.
 */

public class StreamService extends Service implements
        MediaPlayer.OnPreparedListener {//AudioManager.OnAudioFocusChangeListener


    //media player
    private MediaPlayer streamPlayer = new MediaPlayer();
    private String streamUrl;

    //to complete binding process between Service and Activity
    private final IBinder streamBind = new StreamBinder();

    private static final int NOTIFY_ID=1;


    //--------------------------lifecycle methods--------------------------------
    public void onCreate(){
        //create the service
        super.onCreate();
        //ak nie je konektivita tak vypisat chybu

        initMusicPlayer();
//
//        String url = "http://icecast.stv.livebox.sk/slovensko_128.mp3"; // your URL here
//        try {
//            streamPlayer.setDataSource(url);
//            streamPlayer.prepare(); // might take long! (for buffering, etc)
//            streamPlayer.start();
//        } catch(java.io.IOException ioe) {
//            ioe.printStackTrace();
//        } catch(IllegalArgumentException iae) {
//            iae.printStackTrace();
//        }


        }

    @Override
    public IBinder onBind(Intent intent) {

        return streamBind;
    }

    //release resources when the Service instance is unbound
    //This will execute when the user exits the app - it stops the service
    @Override
    public boolean onUnbind(Intent intent){
        streamPlayer.stop();
        streamPlayer.release();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (streamPlayer.isPlaying()) {
            streamPlayer.reset();
            //streamPlayer = null;
        }
    }
    //-------------lifecycle methods END---------------------------

    //------------onPreparedListener mandatory method------------
    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Intent notIntent = new Intent(this, StreamRadioActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //take the user back to the activity class when tap to the notification
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Notification.Builder builder = new Notification.Builder(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker("Streaming radio")
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText("Streaming radio");
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

    public void setStream(String stream){
        streamUrl = stream;
    }

    public void playStream(){
        try {
            //feature request
//            if(streamUrl.contains("m3u")){
//                streamUrl = ParserM3UToURL.parse(streamUrl).get(0);
//            }
            streamPlayer.setDataSource(streamUrl);
            streamPlayer.prepare(); // might take long! (for buffering, etc)
            streamPlayer.start();
        } catch(java.io.IOException ioe) {
            ioe.printStackTrace();
            //vypisat toast message!
        } catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    public void initMusicPlayer(){
        //set player properties
        streamPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        streamPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    //part of the interaction between the Activity and Service classes, for which we also need a Binder instance
    public class StreamBinder extends Binder {
        StreamService getService() {

            return StreamService.this;
        }
    }
}

