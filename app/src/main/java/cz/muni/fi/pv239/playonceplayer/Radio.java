package cz.muni.fi.pv239.playonceplayer;

/**
 * Created by jrumanov on 4/24/15.
 */
public class Radio {

    private int id;
    private String radioname;
    private String uri;

    //constructor method
    public Radio(int id, String radioname, String uri) {
        this.id=id;
        this.radioname=radioname;
        this.uri=uri;
    }

    //-------------getters for attributes------------------
    public int getId(){
        return id;
    }

    public String getRadioname(){
        return radioname;
    }

    public String getUri(){
        return uri;
    }
}

