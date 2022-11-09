package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;
import java.util.Locale;

import static gitlet.Repository.COMMITS_DIR;
import static gitlet.Utils.join;
import static gitlet.Utils.writeObject;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Shuyuan Wang
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private Date date;
    /** The message of this Commit. */
    private String message;
    /** The blobs in this Commit. A Map from filename to SHA1 ID */
    private HashMap<String, String> blobs;
    /** The blobs in this Commit. A Map from filename to SHA1 ID */
    private String parent;

    /** Constructor */
    Commit(Date date, String message, String parent) {
        this.date = date;
        this.message = message;
        this.parent = parent;
        this.blobs = new HashMap<>();
    }

    Commit(Date date, String message, String parent, HashMap<String, String> blobs) {
        this.date = date;
        this.message = message;
        this.parent = parent;
        this.blobs = blobs;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public String getParent() {
        return parent;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedTime() {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return df.format(date);
    }

    public void save(String ID) {
        File commitPrefix = join(COMMITS_DIR, ID.substring(0, 2));  // To accelerate the abbreviation search.
        if (!commitPrefix.exists()) {
            commitPrefix.mkdir();
        }
        writeObject(join(commitPrefix, ID.substring(2)), this);
    }

    public boolean containsFile(String fileName) {
        return blobs.containsKey(fileName);
    }

    public String fileVersion(String fileName) {
        return blobs.get(fileName);
    }

    /** The default Object class' toString() method prints the location of the object in memory */
    public String toString() {
        String s = date.toString() + message + blobs.toString() + parent;
        return s;
    }
}
