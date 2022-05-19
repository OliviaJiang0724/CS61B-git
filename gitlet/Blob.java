package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    /** Save the content of the new file.*/
    private String _content;

    /** save the name of this file.*/
    private String _name;

    public Blob(File newFile, String filename) {
        _name = filename;
        _content = Utils.readContentsAsString(newFile);
    }



    public String getContent() {
        return _content;
    }

    public String getName() {
        return _name;
    }

}
