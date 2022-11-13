package gitlet;

import java.io.*;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  Provides interfaces for all the commands and their helper methods.
 *
 *  @author Shuyuan Wang
 */
public class Repository {
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The .gitlet/objects directory.
     */
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    /**
     * The .gitlet/objects directory.
     */
    public static final File INDEX = join(GITLET_DIR, "INDEX");
    /**
     * The .gitlet/branches directory.
     */
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    /**
     * The .gitlet/objects directory.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /**
     * The .gitlet/objects directory.
     */
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /**
     * The .gitlet/objects directory.
     */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");

    /**
     * init command.
     */
    public static void initRepo() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already " +
                    "exists in the current directory.");
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

    private static void checkIfGitletDir() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /**
     * add command.
     */
    public static void addFile(String fileName) {
        checkIfGitletDir();
        File newFile = join(CWD, fileName);
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        /** Save the blob if it's new. */
        byte[] fileContent = readContents(newFile);
        String ID = sha1(fileContent);
        File blobPrefix = join(BLOBS_DIR, ID.substring(0, 2));
        if (!blobPrefix.exists()) {
            blobPrefix.mkdir();
        }
        File blob = join(blobPrefix, ID.substring(2));
        if (!blob.exists()) {
            writeContents(blob, fileContent);
        }
        /** Update the INDEX if necessary. */
        Commit headCommit = getHeadCommit();
        Index stagingArea = Index.getStagingArea();
        // rm and then add again.
        if (stagingArea.removed.containsKey(fileName)
            && stagingArea.removed.get(fileName).equals(ID)) {
            stagingArea.removed.remove(fileName);
        // New file or modified the first time.
        } else if (!stagingArea.staged.containsKey(fileName)) {
            if (!headCommit.tracks(fileName) || !headCommit.fileVersion(fileName).equals(ID)) {
                stagingArea.staged.put(fileName, ID);
            } else {
                return;
            }
        // Staged and then modified.
        } else if (!stagingArea.staged.get(fileName).equals(ID)) {
            stagingArea.staged.put(fileName, ID);;
        } else {
            return;
        }
        stagingArea.save();
    }

    /**
     * Return the Commit Obj pointed by HEAD.
     */
    private static Commit getHeadCommit() {
        String curBranch = readContentsAsString(HEAD);
        String SHA1 = getHeadCommitID(curBranch);
        return getCommitBySHA(SHA1);
    }

    /**
     * Return the SHA1 value of the head commit in the given branch.
     */
    private static String getHeadCommitID(String branch) {
        return readContentsAsString(join(BRANCHES_DIR, branch));
    }

    /**
     * Return the Commit Obj with the given SHA1 value.
     */
    private static Commit getCommitBySHA(String ID) {
        File commitPrefix = join(COMMITS_DIR, ID.substring(0, 2));
        File commit = join(commitPrefix, ID.substring(2));
        if (!commit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return readObject(commit, Commit.class);
    }

    /**
     * Create a new blob that updates the blob from the given commit
     * with the given Index.
     *
     * @param commit  The commit from which the blob will be read and created.
     * @param changes The Index Object.
     * @Return The updated blobs.
     */
    private static HashMap<String, String> getNewBlobs(Commit commit, Index changes) {
        HashMap<String, String> orig = commit.getBlobs();
        HashMap<String, String> newBlobs = new HashMap<>(orig);

        for (Map.Entry<String, String> entry : changes.staged.entrySet()) {
            newBlobs.put(entry.getKey(), entry.getValue());
        }
        for (String removedFile : changes.removed.keySet()) {
            newBlobs.remove(removedFile);
        }
        return newBlobs;
    }

    /**
     * commit command.
     */
    public static void commit(String message) {
        /** Precheck. */
        checkIfGitletDir();
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Index changes = Index.getStagingArea();
        if (changes.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        /** Construct and save the new commit. */
        // Date.
        Date timeStamp = new Date();
        // Parent.
        String curBranch = readContentsAsString(HEAD);
        String parent = getHeadCommitID(curBranch);
        // Blobs.
        Commit prevCommit = getCommitBySHA(parent);
        HashMap<String, String> newBlobs = getNewBlobs(prevCommit, changes);
        // Create and save the new commit.
        Commit newCommit = new Commit(timeStamp, message, parent, newBlobs);
        String ID = sha1(newCommit.toString());
        newCommit.save(ID);

        /** Update branch and Clear the staging area. */
        writeContents(join(BRANCHES_DIR, curBranch), ID);
        changes.clear();
        changes.save();
    }

    /**
     * rm command.
     */
    public static void remove(String fileName) {
        checkIfGitletDir();
        boolean errorFlag = true;   // Flags: true if the file is neither staged nor tracked.

        Index changes = Index.getStagingArea();
        if (changes.staged.containsKey(fileName)) {
            changes.staged.remove(fileName);
            errorFlag = false;
        }

        Commit headCommit = getHeadCommit();
        if (headCommit.tracks(fileName)) {
            changes.removed.put(fileName, headCommit.fileVersion(fileName));
            File toDelete = join(CWD, fileName);
            if (toDelete.exists() && !restrictedDelete(toDelete)) {
                System.exit(0);
            }
            errorFlag = false;
        }

        if (errorFlag) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        changes.save();
    }

    private static void printCommit(String ID, Commit commit) {
        StringBuilder returnSB = new StringBuilder();
        returnSB.append("===\n");
        returnSB.append("commit " + ID + "\n");
        if (commit.isMergeCommit()) {
            returnSB.append(
                "Merge: " + commit.getParent().substring(0, 7) + " " + commit.getMergeParent().substring(0, 7) + "\n"
            );
        }
        returnSB.append("Date: " + commit.getFormattedTime() + "\n");
        returnSB.append(commit.getMessage() + "\n");
        returnSB.append("\n");
        System.out.print(returnSB.toString());
    }

    /**
     * log command.
     */
    public static void log() {
        checkIfGitletDir();
        String curBranch = readContentsAsString(HEAD);
        String ID = getHeadCommitID(curBranch);
        Commit curCommit = getCommitBySHA(ID);
        printCommit(ID, curCommit);
        String parentID = curCommit.getParent();
        while (parentID != null) {  // The parent of initial commit is null.
            curCommit = getCommitBySHA(parentID);
            ID = parentID;
            printCommit(ID, curCommit);
            parentID = curCommit.getParent();
        }
    }

    /**
     * global-log command.
     */
    public static void globalLog() {
        checkIfGitletDir();
        String[] commitDirs = COMMITS_DIR.list();
        for (String commitDir : commitDirs) {
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

    /**
     * status command.
     */
    public static void status() {
        checkIfGitletDir();
        StringBuilder returnSB = new StringBuilder();

        /** Branches. */
        String curBranch = readContentsAsString(HEAD);
        List<String> branches = plainFilenamesIn(BRANCHES_DIR);
        returnSB.append("=== Branches ===\n");
        returnSB.append("*");
        returnSB.append(curBranch);
        returnSB.append("\n");
        for (String branch : branches) {
            if (!branch.equals(curBranch)) {
                returnSB.append(branch);
                returnSB.append("\n");
            }
        }
        returnSB.append("\n");

        /** Staged Files. */
        Index changes = Index.getStagingArea();

        String[] stagedFiles = changes.staged.keySet().toArray(new String[0]);
        Arrays.sort(stagedFiles);
        returnSB.append("=== Staged Files ===\n");
        for (String stagedFile : stagedFiles) {
            returnSB.append(stagedFile);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        /** Removed Files. */
        String[] removedFiles = changes.removed.keySet().toArray(new String[0]);
        Arrays.sort(removedFiles);
        returnSB.append("=== Removed Files ===\n");
        for (String removedFile : removedFiles) {
            returnSB.append(removedFile);
            returnSB.append("\n");
        }
        returnSB.append("\n");

        /** Modifications Not Staged For Commit. */
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

        /** Untracked Files. */
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

    /**
     * find command.
     */
    public static void find(String message) {
        checkIfGitletDir();
        StringBuilder returnSB = new StringBuilder();

        String[] commitDirs = COMMITS_DIR.list();
        for (String commitDir : commitDirs) {
            List<String> commits = plainFilenamesIn(join(COMMITS_DIR, commitDir));
            for (String commit : commits) {
                String ID = commitDir + commit;
                Commit commitObj = getCommitBySHA(ID);
                if (commitObj.getMessage().contains(message)) { // Use String.contains() method.
                    returnSB.append(ID);
                    returnSB.append("\n");
                }
            }
        }
        if (returnSB.toString().isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        returnSB.append("\n");
        System.out.println(returnSB.toString());
    }

    private static byte[] getBlobContent(String blobID) {
        File blob = join(join(BLOBS_DIR, blobID.substring(0, 2)), blobID.substring(2));
        return readContents(blob);
    }

    private static boolean branchExists(String branchName) {
        File targetBranch = join(BRANCHES_DIR, branchName);
        return targetBranch.exists();
    }

    private static void checkUntrackedOverwritten(List<String> snapShot, HashMap<String, String> newBlobs, Commit targetCommit) {
        for (String fileName : snapShot) {
            if (!newBlobs.containsKey(fileName)) {  // Untracked: neither staged nor tracked.
                if (targetCommit.tracks(fileName)) {  // which will be overwritten.
                    //&& !targetCommit.fileVersion(fileName).equals(curFileVersion(fileName))) {
                    System.out.println("There is an untracked file in the way;" +
                            " delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    public static void checkoutCommit(Commit targetCommit) {
        Commit headCommit = getHeadCommit();

        Index changes = Index.getStagingArea();
        HashMap<String, String> newBlobs = getNewBlobs(headCommit, changes);   // HEAD.blobs + INDEX

        List<String> snapShot = plainFilenamesIn(CWD);

        /** Check if there exists untracked files that will be overwritten. */
        checkUntrackedOverwritten(snapShot, newBlobs, targetCommit);

        /** Delete the files that only tracked by curCommit. */
        for (String fileName : snapShot) {
            if (headCommit.tracks(fileName) && !targetCommit.tracks(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        }
        /** Write the blobs in the target commits to CWD. */
        for (Map.Entry<String, String> entry : targetCommit.getBlobs().entrySet()) {
            File file = join(CWD, entry.getKey());
            String blobID = entry.getValue();
            writeContents(file, getBlobContent(blobID));
        }

        changes.clear();
        changes.save();
    }

    /**
     * checkout [branch name]
     */
    public static void checkoutBranch(String branch) {
        /** Precheck. */
        checkIfGitletDir();
        if (!branchExists(branch)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String curBranch = readContentsAsString(HEAD);
        if (curBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        /** Do the checkout. */
        Commit targetCommit = getCommitBySHA(getHeadCommitID(branch));
        checkoutCommit(targetCommit);

        /** Update HEAD. */
        writeContents(HEAD, branch);
    }

    /**
     * checkout -- [file name]
     */
    public static void checkoutFilefromHEAD(String fileName) {
        checkIfGitletDir();
        Commit headCommit = getHeadCommit();
        checkoutFilefromCommit(headCommit, fileName);
    }

    private static void checkoutFilefromCommit(Commit commit, String fileName) {
        if (!commit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        byte[] content = readBlobContent(commit, fileName);
        writeContents(join(CWD, fileName), content);
    }

    private static byte[] readBlobContent(Commit commit, String fileName) {
        String ID = commit.fileVersion(fileName);
        File file = join(join(BLOBS_DIR, ID.substring(0, 2)), ID.substring(2));
        return readContents(file);
    }

    private static String readBlobContentAsString(Commit commit, String fileName) {
        if (!commit.tracks(fileName)) {
            return "";
        }
        String ID = commit.fileVersion(fileName);
        File file = join(join(BLOBS_DIR, ID.substring(0, 2)), ID.substring(2));
        return readContentsAsString(file);
    }

    /**
     * checkout [commit id] -- [file name]
     */
    public static void checkoutFilefromCommitID(String ID, String fileName) {
        checkIfGitletDir();
        if (ID.length() == 40) {
            checkoutFilefromCommit(getCommitBySHA(ID), fileName);
            return;
        }

        // Abbreviated commit ID.
        File commitPrefix = join(COMMITS_DIR, ID.substring(0, 2));
        if (commitPrefix.exists()) {
            List<String> commitIDs = plainFilenamesIn(commitPrefix);
            for (String commitID : commitIDs) {
                if (commitID.startsWith(ID.substring(2))) {
                    Commit commitObj = readObject(join(commitPrefix, commitID), Commit.class);
                    checkoutFilefromCommit(commitObj, fileName);
                    return;
                }
            }
        }

        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    /**
     * branch command.
     */
    public static void newBranch(String branchName) {
        checkIfGitletDir();
        File branch = join(BRANCHES_DIR, branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        writeContents(branch, getHeadCommitID(readContentsAsString(HEAD)));
    }

    /**
     * rm-branch command.
     */
    public static void removeBranch(String branchName) {
        checkIfGitletDir();
        File branch = join(BRANCHES_DIR, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        branch.delete();
    }

    /**
     * reset command.
     */
    public static void reset(String commitID) {
        checkIfGitletDir();
        File commit = join(join(COMMITS_DIR, commitID.substring(0, 2)), commitID.substring(2));
        if (!commit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        checkoutCommit(getCommitBySHA(commitID));
        String curBranch = readContentsAsString(HEAD);
        writeContents(join(BRANCHES_DIR, curBranch), commitID);
    }

    /**
     * merge command.
     */
    public static void merge(String branchName) {
        /** Precheck. */
        checkIfGitletDir();
        File branch = join(BRANCHES_DIR, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String curBranch = readContentsAsString(HEAD);
        if (curBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Index changes = Index.getStagingArea();
        if (!changes.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }


        /** Given branch and current branch are in one line. */
        String spiltPointID = getSplitPointID(branchName);
        String curCommitID = getHeadCommitID(curBranch);
        String mergedCommitID = getHeadCommitID(branchName);
        if (mergedCommitID.equals(spiltPointID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (curCommitID.equals(spiltPointID)) {
            checkoutCommit(getCommitBySHA(mergedCommitID));
            writeContents(join(BRANCHES_DIR, curBranch), mergedCommitID);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        /** Common cases. */
        Commit splitPoint = getCommitBySHA(spiltPointID);
        Commit curCommit = getCommitBySHA(curCommitID);
        Commit mergedCommit = getCommitBySHA(mergedCommitID);

        checkUntrackedOverwritten(plainFilenamesIn(CWD), curCommit.getBlobs(), mergedCommit);

        HashSet<String> modifiedOrAddInMerge = modifiedOrAddInMergedBranch(splitPoint, curCommit, mergedCommit);
        for (String fileName : modifiedOrAddInMerge) {
            checkoutFilefromCommit(mergedCommit, fileName);
            changes.staged.put(fileName, mergedCommit.fileVersion(fileName));
        }

        HashSet<String> deletedInMerge = deletedInMergedBranch(splitPoint, curCommit, mergedCommit);
        for (String fileName : deletedInMerge) {
            changes.removed.put(fileName, curCommit.fileVersion(fileName));
            restrictedDelete(join(CWD, fileName));
        }

        HashSet<String> bothModified = bothModified(splitPoint, curCommit, mergedCommit);
        if (!bothModified.isEmpty()) {
            for (String fileName : bothModified) {
                writeConflict(fileName, curCommit, mergedCommit);
                changes.staged.put(fileName, sha1(readContents(join(CWD, fileName))));
            }
            System.out.println("Encountered a merge conflict.");
        }

        changes.save();
        Commit mergeCommit = new Commit(
                new Date(),
                "Merged " + branchName + " into " + curBranch + ".",
                new String[] {curCommitID, mergedCommitID},
                getNewBlobs(curCommit, changes)
        );
        String newID = sha1(mergeCommit.toString());
        mergeCommit.save(newID);
        writeContents(join(BRANCHES_DIR, curBranch), newID);
        changes.clear();
        changes.save();
    }

    /**
     * Get the SHA1 ID of the closest common ancestor for curBranch and given branch.
     */
    public static String getSplitPointID(String mergedBranch) {
        HashSet<String> history = new HashSet<>();
        /* Add all the ancestors of curBranch into Set. */
        String curID = getHeadCommitID(readContentsAsString(HEAD));
        history.add(curID);
        Queue<String> q = new LinkedList<>();
        q.add(curID);
        /* BFS. */
        while (!q.isEmpty()) {
            for (int i = 0; i < q.size(); ++i) {
                curID = q.poll();
                Commit curCommit = getCommitBySHA(curID);
                for (String parentID : curCommit.getParents()) {
                    if (parentID != null) {
                        history.add(parentID);
                        q.add(parentID);
                    }
                }
            }
        }
        /** Find the closest ancestor. */
        curID = getHeadCommitID(mergedBranch);
        q.clear();
        q.add(curID);
        while (!q.isEmpty()) {
            for (int i = 0; i < q.size(); ++i) {
                curID = q.poll();
                if (history.contains(curID)) {
                    return curID;
                }
                Commit curCommit = getCommitBySHA(curID);
                for (String parentID : curCommit.getParents())
                    q.add(parentID);
            }
        }
        return curID;
    }

    private static HashSet<String> modifiedOrAddInMergedBranch(Commit splitPoint, Commit curCommit, Commit mergedCommit) {
        HashSet<String> modifiedOrAddInMerge = new HashSet<String>();
        for (Map.Entry<String, String> entry : mergedCommit.getBlobs().entrySet()) {
            if (splitPoint.tracks(entry.getKey()) &&
            !splitPoint.fileVersion(entry.getKey()).equals(entry.getValue()) &&
            curCommit.tracks(entry.getKey()) &&
            curCommit.fileVersion(entry.getKey()).equals(splitPoint.fileVersion(entry.getKey()))) {
                modifiedOrAddInMerge.add(entry.getKey());
            } else if (!splitPoint.tracks(entry.getKey()) && !curCommit.tracks(entry.getKey())) {
                modifiedOrAddInMerge.add(entry.getKey());
            }
        }
        return modifiedOrAddInMerge;
    }

    private static HashSet<String> deletedInMergedBranch(Commit splitPoint, Commit curCommit, Commit mergedCommit) {
        HashSet<String> deletedInMerge = new HashSet<String>();
        for (Map.Entry<String, String> entry : curCommit.getBlobs().entrySet()) {
            if (splitPoint.tracks(entry.getKey()) &&
            !mergedCommit.tracks(entry.getKey()) &&
            curCommit.fileVersion(entry.getKey()).equals(splitPoint.fileVersion(entry.getKey()))) {
                deletedInMerge.add(entry.getKey());
            }
        }
        return deletedInMerge;
    }

    private static HashSet<String> bothModified(Commit splitPoint, Commit curCommit, Commit mergedCommit) {
        HashSet<String> bothModified = new HashSet<String>();
        for (Map.Entry<String, String> entry : curCommit.getBlobs().entrySet()) {
            /** Both add but with diff content. */
            if (!splitPoint.tracks(entry.getKey()) &&
            mergedCommit.tracks(entry.getKey()) &&
            !mergedCommit.fileVersion(entry.getKey()).equals(entry.getValue())) {
                bothModified.add(entry.getKey());
            /** Both modified. */
            } else if (splitPoint.tracks(entry.getKey()) &&
            !splitPoint.fileVersion(entry.getKey()).equals(entry.getValue()) &&
            mergedCommit.tracks(entry.getKey()) &&
            !mergedCommit.fileVersion(entry.getKey()).equals(splitPoint.fileVersion(entry.getKey()))) {
                bothModified.add(entry.getKey());
            /** Modified in curCommit and deleted in mergedCommit. */
            } else if (splitPoint.tracks(entry.getKey()) &&
            !splitPoint.fileVersion(entry.getKey()).equals(entry.getValue()) &&
            !mergedCommit.tracks(entry.getKey())) {
                bothModified.add(entry.getKey());
            }
        }

        for (Map.Entry<String, String> entry : mergedCommit.getBlobs().entrySet()) {
            /** Modified in mergedCommit and deleted in curCommit. */
            if (splitPoint.tracks(entry.getKey()) &&
            !splitPoint.fileVersion(entry.getKey()).equals(entry.getValue()) &&
            !curCommit.tracks(entry.getKey())) {
                bothModified.add(entry.getKey());
            }
        }
        return bothModified;
    }

    private static void writeConflict(String fileName, Commit curCommit, Commit mergedCommit) {
            StringBuilder returnSB = new StringBuilder();
            File conflictFile = join(CWD, fileName);
            returnSB.append("<<<<<<< HEAD\n");
            returnSB.append(readBlobContentAsString(curCommit, fileName));
            returnSB.append("=======\n");
            returnSB.append(readBlobContentAsString(mergedCommit, fileName));
            returnSB.append(">>>>>>>\n");
            writeContents(conflictFile, returnSB.toString());
    }
}
