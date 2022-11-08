package gitlet;

import java.io.*;
import java.util.*;

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
        String ID = sha1(root.toString());
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

    /**
     * Create a new blob that updates the blob from the given commit
     * with the given Index.
     *
     * @param commit The commit from which the blob will be read and created.
     * @param changes The Index Object.
     * @Return The updated blobs.
     */
    private static HashMap<String, String> getNewBlobs(Commit commit, Index changes) {
        HashMap<String, String> orig = commit.getBlobs();
        HashMap<String, String> newBlobs = new HashMap<>(orig);

        for (Map.Entry<String, String> entry : changes.staged.entrySet()) {
            newBlobs.put(entry.getKey(), entry.getValue());
        }
        for (String removedFile : changes.removed) {
            newBlobs.remove(removedFile);
        }
        return newBlobs;
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
        HashMap<String, String> newBlobs = getNewBlobs(prevCommit, changes);

        // Date.
        Date timeStamp = new Date();

        // Create and save the new commit.
        Commit newCommit = new Commit(timeStamp, message, parent, newBlobs);
        String ID = sha1(newCommit.toString());
        newCommit.save(ID);

        // Update branch.
        writeContents(join(BRANCHES_DIR, curBranch), ID);

        // Clear the staging area.
        changes.clear();
        changes.save();
    }

    public static void remove(String fileName) {
        boolean errorFlag = true;
        Index changes = readObject(INDEX, Index.class);
        if (changes.staged.containsKey(fileName)) {
            changes.staged.remove(fileName);
            errorFlag = false;
        }

        Commit headCommit = getHeadCommit();
        if (headCommit.getBlobs().containsKey(fileName)) {
            changes.removed.add(fileName);
            if (!restrictedDelete(join(CWD, fileName))) {
                System.exit(0);
            }
            errorFlag = false;
        }

        if (errorFlag) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    private static void printCommit(String ID, Commit commit) {
        // TODO: merge situation.
        StringBuilder returnSB = new StringBuilder();
        returnSB.append("===\n");
        returnSB.append("commit " + ID + "\n");
        returnSB.append("Date: " + commit.getFormattedTime() + "\n");
        returnSB.append(commit.getMessage() + "\n");
        returnSB.append("\n");
        System.out.print(returnSB.toString());
    }

    public static void log() {
        String curBranch = readContentsAsString(HEAD);
        String ID = getHeadCommitID(curBranch);
        Commit cur = getCommitBySHA(ID);
        String parent = cur.getParent();
        printCommit(ID, cur);
        while (parent != null) {
            cur = getCommitBySHA(parent);
            ID = parent;
            printCommit(ID, cur);
            parent = cur.getParent();
        }
    }

    public static void globalLog() {
        String[] commitDirs = COMMITS_DIR.list();
        for (String commitDir: commitDirs) {
            List<String> commits = plainFilenamesIn(join(COMMITS_DIR, commitDir));
            for (String commit : commits) {
                String ID = commitDir + commit;
                Commit commitObj = getCommitBySHA(ID);
                printCommit(ID, commitObj);
            }
        }
    }

    /**
     * Create a new blobs (HsahMap) for the CWD.
     */
    private static HashMap<String, String> takeSnapShot() {
        List<String> curFiles = plainFilenamesIn(CWD);
        HashMap<String, String> snapShot = new HashMap<>();
        for (String fileName : curFiles) {
            byte[] fileContent = readContents(join(CWD, fileName));
            String ID = sha1(fileContent);
            snapShot.put(fileName, ID);
        }
        return snapShot;
    }

    public static void status() {
        StringBuilder returnSB = new StringBuilder();
        // Branches.
        String curBranch = readContentsAsString(HEAD);
        List<String> branches = plainFilenamesIn(BRANCHES_DIR);
        returnSB.append("=== Branches ===\n");
        returnSB.append("*");
        returnSB.append(curBranch);
        for (String branch : branches) {
            if (branch != curBranch) {
                returnSB.append(branch);
                returnSB.append("\n");
            }
        }
        returnSB.append("\n");

        // Staged Files.
        Index changes = readObject(INDEX, Index.class);

        String[] stagedFiles = (String[]) changes.staged.keySet().toArray();
        Arrays.sort(stagedFiles);
        returnSB.append("=== Staged Files ===\n");
        for (String stagedFile : stagedFiles) {
            returnSB.append(stagedFile);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        // Removed Files.
        String[] removedFiles = (String[]) changes.removed.toArray();
        Arrays.sort(removedFiles);
        returnSB.append("=== Removed Files ===\n");
        for (String removedFile : removedFiles) {
            returnSB.append(removedFile);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        // Modifications Not Staged For Commit.
        // In essence, "modified" means (HEAD.blobs + INDEX) - the same entries in CWD.
        returnSB.append("=== Modifications Not Staged For Commit ===\n");
        HashMap<String, String> newBlobs = getNewBlobs(getHeadCommit(), changes);   // HEAD.blobs + INDEX
        HashMap<String, String> snapShot = takeSnapShot();  // CWD snapshot
        TreeSet<String> modifications = new TreeSet<>();
        for (Map.Entry<String, String> entry : newBlobs.entrySet()) {
            if (snapShot.containsKey(entry.getKey()) && !snapShot.get(entry.getKey()).equals(entry.getValue())) {
                modifications.add(entry.getKey() + " (modified)");
            } else if (!snapShot.containsKey(entry.getKey())) {
                modifications.add(entry.getKey() + " (deleted)");
            }
        }
        for (String entry : modifications) {
            returnSB.append(entry);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        // Untracked Files.
        returnSB.append("=== Untracked Files ===\n");
        TreeSet<String> untracked = new TreeSet<>();
        for (Map.Entry<String, String> entry : snapShot.entrySet()) {
            if (!newBlobs.containsKey(entry.getKey())) {
                untracked.add(entry.getKey());
            }
        }
        for (String entry : untracked) {
            returnSB.append(entry);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        System.out.print(returnSB.toString());
    }
}
