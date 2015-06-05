package cz.muni.fi.pv239.playonceplayer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jrumanov on 4/24/15.
 */
public class Song implements Parcelable{

    private String id;
    private String title;
    private String artist;

    //constructor method
    public Song(String songID, String songTitle, String songArtist) {
        id=songID;
        title=songTitle;
        artist=songArtist;
    }

    //-------------getters for attributes------------------
    public String getID(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    //-------------------parcelable------------------------

    public Song(Parcel in){
        String[] data = new String[3];

        in.readStringArray(data);
        this.id = data[0];
        this.title = data[1];
        this.artist = data[2];
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.id,
                this.title,
                this.artist});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        if (!id.equalsIgnoreCase(song.id)) return false;
        if (title != null ? !title.equalsIgnoreCase(song.title) : song.title != null) return false;
        return !(artist != null ? !artist.equalsIgnoreCase(song.artist) : song.artist != null);

    }

    @Override
    public String toString() {
        return artist + " - " + title;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        return result;
    }

    //------------------CREATOR mandatory method ---------------------------------
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}

