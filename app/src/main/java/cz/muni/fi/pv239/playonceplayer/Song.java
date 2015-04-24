package cz.muni.fi.pv239.playonceplayer;

/**
 * Created by jrumanov on 4/24/15.
 */
public class Song {

    private long id;
    private String title;
    private String artist;

    //constructor method
    public Song(long songID, String songTitle, String songArtist) {
        id=songID;
        title=songTitle;
        artist=songArtist;
    }

    //-------------getters for attributes------------------
    public long getID(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    //add more info about songs later
}
