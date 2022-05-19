package gitlet;



import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
public class Commit implements Serializable {

    /**
     * commit message.
     */
    private String _message;

    /**
     * commit timestamp.
     */
    private String _timestamp;

    /**
     * current working directory's files.
     */
    private HashMap<String, String> fileMap;

    /**
     * current head commitID.
     */
    private String _parent;

    /** second parent ID.*/
    private String _secondParent;

    /** branch name.*/
    private String _branch;

    /** commit hashcode.*/
    private String _hashcode;


    public Commit() {
        this._message = "initial commit";
        Date tmp = new Date(0);
        SimpleDateFormat myFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        this._timestamp = myFormat.format(tmp);
        this._parent = null;
        this._branch = "master";
        this._secondParent = null;
        this._hashcode = null;
    }

    public Commit copyCommit(String message) {
        Commit newCommit = new Commit();
        newCommit._message = message;
        Date tmp = new Date();
        SimpleDateFormat myFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        newCommit._timestamp = myFormat.format(tmp);
        newCommit.fileMap = this.fileMap;
        return newCommit;

    }


    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        this._message = message;
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public String getParent() {
        return _parent;
    }

    public void setParent(String parent) {
        this._parent = parent;
    }

    public HashMap<String, String> getFile() {
        return fileMap;
    }

    public void setFile(HashMap<String, String> newHashMap) {
        this.fileMap = newHashMap;
    }

    public void setBranch(String branchName) {
        this._branch = branchName;
    }


    public void setSecondParent(String secondParent) {
        this._secondParent = secondParent;
    }

    public String getSecondParent() {
        return this._secondParent;
    }

    public String getBranch() {
        return this._branch;
    }

    public void setHashCode(String hash) {
        this._hashcode = hash;
    }
    public String getHashCode() {
        return this._hashcode;
    }

}


