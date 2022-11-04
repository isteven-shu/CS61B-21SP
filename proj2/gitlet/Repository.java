package gitlet;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    /** The .gitlet/objects directory. */
    public static final File INDEX = join(GITLET_DIR, "INDEX");
    /** The .gitlet/branches directory. */
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    /** The .gitlet/objects directory. */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The .gitlet/objects directory. */
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /** The .gitlet/objects directory. */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");

    /** init command */
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
        String ID = sha1(root);
        root.save(ID);

        /** Save master branch and HEAD */
        writeContents(join(BRANCHES_DIR, "master"), ID);
        writeContents(HEAD, "master");
    }

    /** add command */
    public static void AddFile(String fileName) {
        File newFile = join(CWD, fileName);
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        /** Save the blob */
        byte[] fileContent = readContents(newFile);
        String ID = sha1(fileContent);
        File blobPrefix = join(BLOBS_DIR, ID.substring(0, 2));
        if (!blobPrefix.exists()) {
            blobPrefix.mkdir();
        }
        writeContents(join(blobPrefix, ID.substring(2)), fileContent);
        /** Update the INDEX */
        Index stagingArea = null;
        if (INDEX.exists()) {
            stagingArea = readObject(INDEX, Index.class);
        } else {
            stagingArea = new Index();
        }
        stagingArea.staged.put(fileName, ID);
        writeObject(INDEX, stagingArea);
    }

    private static Commit getHeadCommit() {
        String curBranch = readContentsAsString(HEAD);
        String SHA1 = getHeadCommitID(curBranch);
        return getCommitBySHA(SHA1);
    }

    private static String getHeadCommitID(String branch) {
        return readContentsAsString(join(BRANCHES_DIR, branch));
    }

    private static Commit getCommitBySHA(String ID) {
        File commitPrefix = join(COMMITS_DIR, ID.substring(0, 2));
        File commit = join(commitPrefix, ID.substring(2));
        if (!commit.exists()) {
            System.exit(0);
        }
        return readObject(commit, Commit.class);
    }

    /** commit command */
    public static void NewCommit(String message) {
        if (message == null) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Index changes = readObject(INDEX, Index.class);
        if (changes.removed.isEmpty() && changes.staged.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // Parent.
        String curBranch = readContentsAsString(HEAD);
        String parent = getHeadCommitID(curBranch);

        // Blobs.
        Commit prevCommit = getCommitBySHA(parent);
        HashMap<String, String> orig = prevCommit.getBlobs();
        HashMap<String, String> newBlobs = new HashMap<>(orig);
        for (Map.Entry<String, String> entry : changes.staged.entrySet()) {
            newBlobs.put(entry.getKey(), entry.getValue());
        }
        for (String removedFile : changes.removed) {
            newBlobs.remove(removedFile);
        }

        // Date.
        Date timeStamp = new Date();

        // Create and save the new commit.
        Commit newCommit = new Commit(timeStamp, message, parent, newBlobs);
        String ID = sha1(newCommit);
        newCommit.save(ID);

        // Update branch.
        writeContents(join(BRANCHES_DIR, curBranch), ID);

        // Clear the staging area.
        changes.clear();
        changes.save();
    }
}
