package gitlet;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Shuyuan Wang
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .gitlet/objects directory. */
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    /** The .gitlet/branches directory. */
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    /** The .gitlet/objects directory. */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The .gitlet/objects directory. */
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /** The .gitlet/objects directory. */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");

    public static void InitRepo() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        /** Create the directories */
        GITLET_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        COMMITS_DIR.mkdir();

        /** Create and save initial commit */
        Commit root = new Commit(new Date(0), "initial commit", null);
        String ID = sha1(root.toString());
        File commitPrefix = join(COMMITS_DIR, ID.substring(0, 2));  // To accelerate the abbreviation search.
        commitPrefix.mkdir();
        writeObject(join(commitPrefix, ID.substring(2)), root);

        /** Save master branch and HEAD */
        writeContents(join(BRANCHES_DIR, "master"), ID);
        writeContents(join(GITLET_DIR, "HEAD"), "master");
    }
}
