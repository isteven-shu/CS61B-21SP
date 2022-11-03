package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;

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
    }
}
