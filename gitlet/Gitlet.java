package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayDeque;

public class Gitlet implements Serializable {
    /** HashMap to save record the addition Stage.*/
    private HashMap<String, String> additionStage;

    /** HashMap to record the removalStage.*/
    private HashMap<String, String> removalStage;

    /** HashMap to record All the Blob Content.*/
    private  final HashMap<String, String> blobMap;

    /** Current active Branch.*/
    private String currentBranch;

    /** String that save the commitID of head commit.*/
    private String head;

    /** root path of the gitlet directory.*/
    private final String rootPath = ".gitlet"
            + System.getProperty("file.separator");

    /** creat a new gitlet directory.*/
    private final File gitlet = new File(rootPath);

    /** create a new Commit folder to save all commit object.*/
    private final File commitFolder = Utils.join(gitlet, ".commit",
            System.getProperty("file.separator"));

    /** create a new Blob folder to save all blob object.*/
    private final File blobFolder = Utils.join(gitlet, ".blob",
            System.getProperty("file.separator"));

    /** Boolean represents whether you have inited.*/
    private  boolean inited;

    /** ArrayList to save all the branch name.*/
    private ArrayList<String> branchName;

    /** HashMap to save the branch and its commit list.*/
    private HashMap<String, ArrayList<String>> branchMap;

    /** ArrayList to save all the commitID.*/
    private ArrayList<String> commitMessage = new ArrayList<>();

    /** Current working directory.*/
    private String cwd = System.getProperty("user.dir");

    /** ArrayList save all commit ID.*/
    private ArrayList<String> allCommitId;



    public Gitlet() {
        inited = false;
        head = null;
        currentBranch = null;
        blobMap = new HashMap<>();
        removalStage = new HashMap<>();
        additionStage = new HashMap<>();
        gitlet.mkdir();
        commitFolder.mkdir();
        blobFolder.mkdir();
        currentBranch = "master";
        branchName = new ArrayList<>();
        branchMap = new HashMap<>();
        allCommitId = new ArrayList<>();
        conflictFile = new ArrayList<>();
        remoteRepository = new HashMap<>();
    }


    public void run(String... args) {
        switch (args[0]) {
            case "init" -> init();
            case "add" -> add(args[1]);
            case "commit" -> commit(args[1]);
            case "rm" -> remove(args[1]);
            case "log" -> log();
            case "global-log" -> globallog();
            case "find" -> find(args[1]);
            case "status" -> status();
            case "add-remote" -> addRemote(args[1], args[2]);
            case "rm-remote" -> removeRemote(args[1]);
            case "push" -> push(args[1], args[2]);
            case "fetch" -> fetch(args[1], args[2]);
            case "pull" -> pull(args[1], args[2]);
            case "checkout" -> runCheckout(args);
            case "branch" -> branch(args[1]);
            case "rm-branch" -> removeBranch(args[1]);
            case "reset" -> reset(args[1]);
            case "merge" -> merge(args[1]);
            default -> System.out.println("No command with that name exists.");
        }
    }
    public void runCheckout(String... args) {
        if (args[1].equals("--")) {
            checkout(head, args[2]);
        } else if (args.length == 2) {
            checkout2(args[1]);
        } else if (args.length == 4 & args[2].equals("--")) {
            if (args[1].length() == 8) {
                checkoutShort(args[1], args[3]);
            } else {
                checkout(args[1], args[3]);
            }
        } else {
            System.out.println("Incorrect operands.");
        }
    }
    public void init() {
        if (inited) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");

            return;
        } else {
            Commit initialCommit = new Commit();
            head = Utils.sha1(Utils.serialize(initialCommit));
            inited = true;

            allCommitId.add(head);
            File path = Utils.join(commitFolder, head);
            Utils.writeObject(path, initialCommit);
            commitMessage.add(head);
            branchName.add(currentBranch);
            ArrayList<String> commitBranch = new ArrayList<>();
            commitBranch.add(head);
            branchMap.put(currentBranch, commitBranch);

        }
    }

    public void add(String fileName) {
        File newFile = Utils.join(cwd, fileName);
        if (newFile.exists()) {
            Blob newBlob = new Blob(newFile, fileName);

            String hashCode = Utils.sha1(Utils.serialize(newBlob));
            File blobPath = Utils.join(blobFolder, hashCode);
            Utils.writeObject(blobPath, newBlob);

            blobMap.put(hashCode, newBlob.getContent());
            File headPath = Utils.join(commitFolder, head);
            Commit headCommit = Utils.readObject(headPath, Commit.class);
            HashMap<String, String> trackedFile = headCommit.getFile();

            additionStage.put(fileName, hashCode);

            if (trackedFile != null && trackedFile.containsKey(fileName)
                    && trackedFile.get(fileName).equals(hashCode)) {
                additionStage.remove(fileName, hashCode);
            }
            if (removalStage.containsKey(fileName)) {
                removalStage.remove(fileName);
                removalStage.remove(fileName);
            }
        } else {
            System.out.println("File does not exist.");
        }
    }

    public void remove(String fileName) {


        File headPath = Utils.join(commitFolder, head);
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        HashMap<String, String> trackedFile = headCommit.getFile();
        if (additionStage.containsKey(fileName)) {
            additionStage.remove(fileName);
        } else if (trackedFile != null && trackedFile.containsKey(fileName)) {
            removalStage.put(fileName, trackedFile.get(fileName));
            Utils.restrictedDelete(fileName);

        } else {
            System.out.println("No reason to remove the file.");
            return;
        }

    }

    public void commit(String message) {
        if (message.equals("")) {
            System.out.println(("Please enter a commit message."));
            return;
        }

        File headPath = Utils.join(commitFolder, head);
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        Commit newCommit = headCommit.copyCommit(message);
        if (headCommit.getHashCode() == null) {
            newCommit.setParent(Utils.sha1(Utils.serialize(headCommit)));
        } else {
            newCommit.setParent(headCommit.getHashCode());
        }
        newCommit.setMessage(message);
        newCommit.setBranch(currentBranch);
        HashMap<String, String> thisFile = new HashMap<>();
        if (newCommit.getFile() != null) {
            for (Map.Entry<String, String> set
                    : headCommit.getFile().entrySet()) {
                thisFile.put(set.getKey(), set.getValue());
            }
        }

        for (Map.Entry<String, String> set: additionStage.entrySet()) {
            thisFile.put(set.getKey(), set.getValue());
        }
        for (Map.Entry<String, String> set: removalStage.entrySet()) {
            thisFile.remove(set.getKey(), set.getValue());
        }

        if (thisFile.isEmpty() && headCommit.getFile() == null
                || thisFile.equals(headCommit.getFile())) {
            System.out.println(("No changes added to the commit."));
            return;
        }
        newCommit.setFile(thisFile);
        additionStage = new HashMap<>();
        removalStage = new HashMap<>();
        head = Utils.sha1(Utils.serialize(newCommit));
        File path = Utils.join(commitFolder, head);
        Utils.writeObject(path, newCommit);
        commitMessage.add(head);
        ArrayList<String> commitBranch = branchMap.get(currentBranch);
        commitBranch.add(head);
        branchMap.put(currentBranch, commitBranch);
        allCommitId.add(head);
    }


    public  void find(String message) {
        boolean find = false;
        for (String s: commitMessage) {
            File path = Utils.join(commitFolder, s);
            Commit currentCommit = Utils.readObject(path, Commit.class);
            if (currentCommit.getMessage().equals(message)) {
                System.out.println(s);
                find = true;
            }
        }
        if (!find) {
            System.out.println("Found no commit with that message");
        }
    }
    public void checkoutShort(String shortMessage, String fileName) {
        for (String s : allCommitId) {
            String sub = s.substring(0, 8);
            if (sub.equals(shortMessage)) {
                checkout(s, fileName);
                break;
            }
        }
    }
    public void checkout(String mess1, String fileName) {
        boolean find = false;
        File current = Utils.join(commitFolder, mess1);
        if (!current.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit currentCommit = Utils.readObject(current, Commit.class);

        HashMap<String, String> fileMap = currentCommit.getFile();
        for (Map.Entry<String, String> set: fileMap.entrySet()) {
            if (set.getKey().equals(fileName)) {
                find = true;
                File findFile = Utils.join(blobFolder, set.getValue());
                Blob blob = Utils.readObject(findFile, Blob.class);
                findFile = new File(fileName);
                Utils.writeContents(findFile, blob.getContent());
            }
        }
        if (!find) {
            System.out.println("File does not exist in that commit.");
        }

    }
    public void checkout2(String name) {
        if (!branchName.contains(name)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (currentBranch.equals(name)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        ArrayList<String> thisbranch = branchMap.get(name);
        String branchHead = thisbranch.get(thisbranch.size() - 1);
        File checkoutPath = Utils.join(commitFolder, branchHead);
        Commit checkoutCommit = Utils.readObject(checkoutPath, Commit.class);
        HashMap<String, String> fileMap = checkoutCommit.getFile();
        File currentPath = Utils.join(commitFolder, head);
        Commit currentCommit = Utils.readObject(currentPath, Commit.class);
        HashMap<String, String> currentFileMap = currentCommit.getFile();
        List<String> filecwd = Utils.plainFilenamesIn(cwd);
        additionStage = new HashMap<>();
        removalStage = new HashMap<>();
        for (String s : filecwd) {
            File findFile = Utils.join(cwd, s);
            if (currentFileMap == null || !currentFileMap.containsKey(s)) {
                Blob blob = new Blob(findFile, s);
                if (fileMap != null && !Utils.sha1(
                        Utils.serialize(blob)).equals(fileMap.get(s))
                        && fileMap.containsKey(s)) {
                    System.out.println(unTrackedError);
                }
            }
            if (fileMap == null || !fileMap.containsKey(s)) {
                Utils.restrictedDelete(findFile);
            }
        }
        if (fileMap != null) {
            for (Map.Entry<String, String> set : fileMap.entrySet()) {
                File fileInCommit = Utils.join(blobFolder, set.getValue());
                Blob blob = Utils.readObject(fileInCommit, Blob.class);
                if (filecwd.contains(set.getKey())) {
                    File fileIncwd = Utils.join(cwd, set.getKey());
                    Blob blobcwd = new Blob(fileIncwd, set.getKey());
                    if (!blobcwd.equals(blob)) {
                        Utils.restrictedDelete(fileIncwd);
                    }
                }
                File file = Utils.join(cwd, set.getKey());
                String newContent = blobMap.get(
                        Utils.sha1(Utils.serialize(blob)));
                Utils.writeContents(file, newContent);
            }
        }
        currentBranch = name;
        if (checkoutCommit.getHashCode() == null) {
            head = Utils.sha1(Utils.serialize(checkoutCommit));
        } else {
            head = checkoutCommit.getHashCode();
        }
    }

    public  void log() {
        int count = 0;
        File headPath = Utils.join(commitFolder, head);
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        if (headCommit.getMessage().equals("initial commit")) {
            count = 1;
        }
        System.out.println("===");
        System.out.println("commit " + head);
        System.out.println("Date: " + headCommit.getTimestamp());
        System.out.println(headCommit.getMessage());
        System.out.println();
        while (headCommit.getParent() != null
                && (!(headCommit.getMessage().equals("initial commit"))
                && count < 2)) {
            File parentPath = Utils.join(commitFolder, headCommit.getParent());
            Commit parentCommit = Utils.readObject(parentPath, Commit.class);
            System.out.println("===");
            System.out.println("commit " + headCommit.getParent());
            System.out.println("Date: " + parentCommit.getTimestamp());
            System.out.println(parentCommit.getMessage());
            System.out.println();
            headCommit = parentCommit;
        }

    }

    public  void globallog() {
        List<String> commitFile = Utils.plainFilenamesIn(commitFolder);
        for (String commitName : commitFile) {
            File path = Utils.join(commitFolder, commitName);
            Commit current = Utils.readObject(path, Commit.class);
            System.out.println("===");
            System.out.println("commit " + commitName);
            System.out.println("Date: " + current.getTimestamp());
            System.out.println(current.getMessage());
            System.out.println();
        }
    }

    public  void status() {
        if (!inited) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        File headPath = Utils.join(commitFolder, head);
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        List<String> filecwd = Utils.plainFilenamesIn(cwd);
        HashMap<String, String> thisFile = new HashMap<>();
        if (headCommit.getFile() != null) {
            thisFile = headCommit.getFile();
        }
        ArrayList<String> stagedFile = new ArrayList<>();
        ArrayList<String> removedFile = new ArrayList<>();
        ArrayList<String> modifiedFile = new ArrayList<>();
        ArrayList<String> unTrackFile = new ArrayList<>();
        ArrayList<String> deletedFile = new ArrayList<>();
        helperStatus(filecwd, thisFile, unTrackFile, modifiedFile, stagedFile);
        for (Map.Entry<String, String> set: additionStage.entrySet()) {
            String fileName = set.getKey();
            if (!filecwd.contains(fileName)) {
                modifiedFile.add(fileName);
            }
        }
        for (Map.Entry<String, String> set: thisFile.entrySet()) {
            String fileName = set.getKey();
            if (!removalStage.containsKey(fileName)
                    && !filecwd.contains(fileName)) {
                deletedFile.add(fileName);
            }
        }
        for (Map.Entry<String, String> set: removalStage.entrySet()) {
            String fileName = set.getKey();
            removedFile.add(fileName);
        }
        helperStatus2(removedFile, unTrackFile,
                modifiedFile, stagedFile, deletedFile);
    }
    public void helperStatus2(ArrayList<String> removedFile,
                              ArrayList<String> unTrackFile,
                              ArrayList<String> modifiedFile,
                              ArrayList<String> stagedFile,
                              ArrayList<String> deletedFile) {
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch);
        if (branchName.size() > 1) {
            for (String s : branchName) {
                if (!s.equals(currentBranch)) {
                    System.out.println(s);
                }
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String element : stagedFile) {
            System.out.println(element);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String item : removedFile) {
            System.out.println(item);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String value : modifiedFile) {
            String s = value + " (modified)";
            System.out.println(s);
        }
        for (String value : deletedFile) {
            String s = value + " (deleted)";
            System.out.println(s);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String s : unTrackFile) {
            System.out.println(s);
        }
        System.out.println();
    }



    public void helperStatus(List<String> filecwd,
                             HashMap<String, String> thisFile,
                             ArrayList<String> unTrackFile,
                             ArrayList<String> modifiedFile,
                             ArrayList<String> stagedFile) {
        for (String s : filecwd) {
            File cwdFile = new File(s);
            Blob cwdBlob = new Blob(cwdFile, s);
            String cwdHashCode = Utils.sha1(Utils.serialize(cwdBlob));
            if (!additionStage.containsKey(s)) {
                if (!thisFile.containsKey(s)) {
                    unTrackFile.add(s);
                } else if (thisFile.containsKey(s)
                        && !thisFile.get(s).equals(cwdHashCode)) {
                    modifiedFile.add(s);
                }
            }
            if (additionStage.containsKey(s)) {
                if (additionStage.get(s).equals(cwdHashCode)) {
                    stagedFile.add(s);
                } else {
                    modifiedFile.add(s);
                }
            }
        }
    }




    public void branch(String name) {
        if (branchName.contains(name)) {
            System.out.println(" A branch with that name already exists.");
        } else {
            branchName.add(name);
            ArrayList<String> newBranch = new ArrayList<>();
            newBranch.add(head);
            branchMap.put(name, newBranch);
        }
    }

    public void removeBranch(String name) {
        if (currentBranch.equals(name)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!branchName.contains(name)) {
            System.out.println("branch with that name does not exist.");
        } else {
            branchName.remove(name);
            branchMap.remove(name);
        }
    }

    public  void reset(String commitID) {
        if (!allCommitId.contains(commitID)) {
            System.out.println("No commit with that id exists.");
        } else {
            Commit resetCommit = getCommit(commitFolder, commitID);
            Commit headCommit = getCommit(commitFolder, head);
            HashMap<String, String> resetFile = resetCommit.getFile();
            HashMap<String, String> headFile = headCommit .getFile();
            List<String> fileIncwd = Utils.plainFilenamesIn(cwd);
            for (String s : fileIncwd) {
                if (!headFile.containsKey(s) && resetFile.containsKey(s)) {
                    System.out.println(unTrackedError);
                }

                if (!resetFile.containsKey(s) && headFile.containsKey(s)) {
                    Utils.restrictedDelete(Utils.join(cwd, s));
                }

            }

            for (String s : resetFile.keySet()) {
                String content = getBlobContent(resetFile.get(s));
                Utils.writeContents(Utils.join(cwd, s), content);
            }

            additionStage = new HashMap<>();
            removalStage = new HashMap<>();
            head = commitID;
            ArrayList<String> branchList = branchMap.get(currentBranch);
            branchList.add(head);
        }

    }

    public Boolean checkMergeError(String name) {
        if (!branchName.contains(name)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (name.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        if (!additionStage.isEmpty() || !removalStage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        return false;
    }
    public void merge(String name) {
        boolean result = checkMergeError(name);
        if (result) {
            return;
        }
        String message = String.format("Merged %s into %s.",
                name, currentBranch);
        ArrayList<String> mergingBranch = branchMap.get(name);
        String mergingHead = mergingBranch.get(mergingBranch.size() - 1);
        Commit current =  getCommit(commitFolder, head);
        Commit merge =  getCommit(commitFolder, mergingHead);
        Commit splitPoint = getCommit(commitFolder,
                getSplitPoint(mergingHead, head));
        if (branchMap.get(currentBranch).contains(mergingHead)) {
            System.out.println(errorTwo);
            return;
        }
        if (merge.getParent().contains(head)) {
            System.out.println("Current branch fast-forwarded.");
        }
        HashMap<String, String> mergingFile = merge.getFile();
        HashMap<String, String> headFile = current.getFile();
        HashMap<String, String> splitFile = splitPoint.getFile();
        HashMap<String, String> newFile = new HashMap<>();
        boolean cwdResult = checkcwd(mergingFile, headFile);
        if (cwdResult) {
            return;
        }
        if (splitFile != null) {
            helperMerge(mergingFile, headFile, splitFile, newFile);
        }
        for (String s : headFile.keySet()) {
            if (splitFile == null && mergingFile.keySet().contains(s)
                    && !mergingFile.get(s).equals(headFile.get(s))) {
                encounter(false, s, headFile, mergingFile, newFile);
            } else if (splitFile == null || !splitFile.keySet().contains(s)) {
                newFile.put(s, headFile.get(s));
            }
        }
        for (String s : mergingFile.keySet()) {
            if ((splitFile == null || !splitFile.keySet().contains(s))
                    && (conflictFile == null || !conflictFile.contains(s))) {
                newFile.put(s, mergingFile.get(s));
            }
        }
        commitMerge(message, newFile, head, mergingHead, currentBranch);
        List<String> filecwd = Utils.plainFilenamesIn(cwd);
        for (String s: filecwd) {
            if (!newFile.containsKey(s) && (conflictFile == null
                    || !conflictFile.contains(s))) {
                Utils.restrictedDelete(Utils.join(cwd, s));
            }
        }
        for (String s: newFile.keySet()) {
            if ((conflictFile == null || !conflictFile.contains(s))) {
                Utils.writeContents(Utils.join(cwd, s),
                        blobMap.get(newFile.get(s)));
            }
        }
    }
    public boolean checkcwd(HashMap<String, String> mergingFile,
                            HashMap<String, String> headFile) {
        List<String> filecwd = Utils.plainFilenamesIn(cwd);
        for (String s : filecwd) {
            if (mergingFile.containsKey(s) && !headFile.containsKey(s)) {
                Blob blobcwd = new Blob(Utils.join(cwd, s), s);
                if (!mergingFile.get(s).equals(
                        Utils.sha1(Utils.serialize(blobcwd)))) {
                    System.out.println(unTrackedError);
                    return true;
                }
            }
        }
        return false;
    }

    public void helperMerge(HashMap<String, String> mergingFile,
                            HashMap<String, String> headFile,
                            HashMap<String, String> splitFile,
                            HashMap<String, String> newFile) {
        for (String s : splitFile.keySet()) {
            if (mergingFile.keySet().contains(s)) {
                if (headFile.keySet().contains(s)) {
                    if (mergingFile.get(s).equals(headFile.get(s))) {
                        newFile.put(s, mergingFile.get(s));
                    } else if (!mergingFile.get(s).equals(headFile.get(s))) {
                        if (splitFile.get(s).equals(headFile.get(s))) {
                            newFile.put(s, mergingFile.get(s));
                        } else if (splitFile.get(s).equals(
                                mergingFile.get(s))) {
                            newFile.put(s, headFile.get(s));
                        } else {
                            encounter(false, s, headFile, mergingFile, newFile);
                        }
                    }
                } else if (!splitFile.get(s).equals(mergingFile.get(s))) {
                    encounter(true, s, headFile, mergingFile, newFile);
                }
            }
            if (headFile.keySet().contains(s)
                    && !mergingFile.keySet().contains(s)) {
                if (!splitFile.get(s).equals(headFile.get(s))) {
                    encounter(true, s, headFile, mergingFile, newFile);
                }
            }
        }
    }

    public String getSplitPoint(String activeHead, String checkoutHead) {
        ArrayList<String> flag = flagBuffer(activeHead);
        Queue<String> bfsBuffer = new ArrayDeque<>();
        if (flag.contains(checkoutHead)) {
            return checkoutHead;
        }
        bfsBuffer.add(checkoutHead);

        while (bfsBuffer.size() != 0) {
            String currentCommitID = bfsBuffer.remove();
            Commit currentCommit = getCommit(commitFolder, currentCommitID);
            if (flag.contains(currentCommitID)) {
                return currentCommitID;
            }
            if (currentCommit.getSecondParent() != null) {
                bfsBuffer.add(currentCommit.getSecondParent());
            }
            if (currentCommit.getParent() != null) {
                bfsBuffer.add(currentCommit.getParent());
            }
        }
        System.out.println("exist error");
        return null;

    }
    public ArrayList<String> flagBuffer(String thisHead) {
        ArrayList<String> flag = new ArrayList<>();
        Queue<String> bfsBuffer = new ArrayDeque<>();
        bfsBuffer.add(thisHead);

        while (bfsBuffer.size() != 0) {
            String currentCommitID = bfsBuffer.remove();
            Commit currentCommit = getCommit(commitFolder, currentCommitID);
            if (currentCommit.getParent() != null) {
                bfsBuffer.add(currentCommit.getParent());

            }
            if (currentCommit.getSecondParent() != null) {
                bfsBuffer.add(currentCommit.getSecondParent());
            }
            flag.add(currentCommitID);
        }
        return flag;
    }
    public Commit getCommit(File folder, String commitID) {
        File path = Utils.join(folder, commitID);
        Commit newCommit = Utils.readObject(path, Commit.class);
        return newCommit;
    }

    public void commitMerge(String message, HashMap<String, String> currentFile,
                            String currentHead,
                            String checkoutHead, String cBranch) {
        Commit headCommit = getCommit(commitFolder, head);
        Commit newCommit = headCommit.copyCommit(message);
        newCommit.setSecondParent(checkoutHead);
        newCommit.setParent(currentHead);
        newCommit.setMessage(message);
        newCommit.setBranch(cBranch);
        newCommit.setFile(currentFile);
        head = Utils.sha1(Utils.serialize(newCommit));
        File path = Utils.join(commitFolder, head);
        Utils.writeObject(path, newCommit);
        commitMessage.add(head);
        ArrayList<String> commitBranch = branchMap.get(cBranch);
        commitBranch.add(head);
        branchMap.put(cBranch, commitBranch);
        allCommitId.add(head);
    }
    public void encounter(boolean delete, String s,
                          HashMap<String, String> fileMap1,
                          HashMap<String, String> fileMap2,
                          HashMap<String, String> newFile) {
        System.out.println("Encountered a merge conflict.");
        Utils.restrictedDelete(Utils.join(cwd, s));
        writeConflict(delete, fileMap1.get(s), fileMap2.get(s), s, newFile);
    }

    public void writeConflict(boolean delete, String hashcode1,
                              String hashcode2, String fileName,
                              HashMap<String, String> newFile) {
        String concat;
        if (delete) {
            concat = "<<<<<<< HEAD\n"
                    + getBlobContent(hashcode1)
                    + "=======\n"
                    + ">>>>>>>\n";
        } else {
            concat = "<<<<<<< HEAD\n"
                + getBlobContent(hashcode1)
                + "=======\n" + getBlobContent(hashcode2)
                + ">>>>>>>\n";
        }
        Utils.writeContents(Utils.join(cwd, fileName), concat);
        Blob blob = new Blob(Utils.join(cwd, fileName), fileName);
        Utils.writeObject(Utils.join(blobFolder,
                Utils.sha1(Utils.serialize(blob))), blob);
        newFile.put(fileName, Utils.sha1(Utils.serialize(blob)));
        conflictFile.add(fileName);
    }
    public String getBlobContent(String hashcode) {
        File path = Utils.join(blobFolder, hashcode);
        Blob blob = Utils.readObject(path, Blob.class);
        return blob.getContent();
    }

    public void addRemote(String name, String directory) {
        if (remoteRepository.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        directory.replace("/", File.separator);
        remoteRepository.put(name, directory);
    }

    public void removeRemote(String name) {
        if (!remoteRepository.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteRepository.remove(name);
    }
    public void fetch(String name, String bName) {
        if (remoteRepository.containsKey(name)
                && new File(remoteRepository.get(name)).exists()) {
            File remotePath = Utils.join(remoteRepository.get(name));
            Gitlet remote = Utils.readObject(Utils.join(remotePath,
                    "gitlet"), Gitlet.class);

            if (!remote.branchName.contains(bName)) {
                System.out.println("That remote does not have that branch.");
                return;
            }
            Stack<Commit> buffer = new Stack<>();
            String newName = name + File.separator + bName;
            if (!branchName.contains(newName)) {
                branch(newName);
            }
            ArrayList<String> remoteBranch = remote.branchMap.get(bName);
            String remoteHead = remoteBranch.get(remoteBranch.size() - 1);
            File commitfolder = Utils.join(remotePath, ".commit");
            File blobfolder = Utils.join(remotePath, ".blob");
            ArrayList<String> commitList = branchMap.get(newName);
            while (remoteHead != null) {
                Commit remoteCommit = Utils.readObject(
                        Utils.join(commitfolder, remoteHead), Commit.class);
                if (!commitList.contains(remoteHead)) {
                    buffer.push(remoteCommit);
                }
                remoteHead = remoteCommit.getParent();
            }
            String currentHead = commitList.get(commitList.size() - 1);
            while (!buffer.isEmpty()) {
                Commit current = buffer.pop();
                HashMap<String, String> fileMap = current.getFile();
                if (fileMap != null) {
                    for (String s : fileMap.keySet()) {
                        Blob blob = Utils.readObject(Utils.join(
                                blobfolder, fileMap.get(s)), Blob.class);
                        Utils.writeObject(Utils.join(blobFolder,
                                fileMap.get(s)), blob);
                        blobMap.put(fileMap.get(s), blob.getContent());
                    }
                }
                String hashCode = Utils.sha1(Utils.serialize(current));
                current.setParent(currentHead);
                current.setBranch(newName);
                current.setHashCode(hashCode);
                currentHead = hashCode;
                Utils.writeObject(Utils.join(commitFolder, hashCode), current);
                allCommitId.add(hashCode);
                commitList.add(hashCode);
            }
        } else {
            System.out.println("Remote directory not found.");
        }
    }
    public void push(String name, String bName) {
        if (remoteRepository.containsKey(name)
                && new File(remoteRepository.get(name)).exists()) {
            File remotePath = Utils.join(remoteRepository.get(name));
            Gitlet remote = Utils.readObject(Utils.join(remotePath,
                    "gitlet"), Gitlet.class);
            if (!allCommitId.contains(remote.head)) {
                System.out.println("Please pull down remote "
                        + "changes before pushing.");
                return;
            }
            Stack<Commit> buffer = new Stack<>();
            String headCommitID = head;
            while (!remote.allCommitId.contains(headCommitID)) {
                Commit headCommit = getCommit(commitFolder, headCommitID);
                buffer.push(headCommit);
                headCommitID = headCommit.getParent();
            }
            File commitfolder = Utils.join(remotePath, ".commit");
            File blobfolder = Utils.join(remotePath, ".blob");
            while (!buffer.isEmpty()) {
                Commit current = buffer.pop();
                HashMap<String, String> fileMap = current.getFile();

                if (fileMap != null) {
                    for (String s : fileMap.keySet()) {
                        Blob blob = Utils.readObject(Utils.join(
                                blobFolder, fileMap.get(s)), Blob.class);
                        Utils.writeObject(Utils.join(blobfolder,
                                fileMap.get(s)), blob);
                        remote.blobMap.put(fileMap.get(s), blob.getContent());
                    }
                }
                String hashCode = Utils.sha1(Utils.serialize(current));
                remote.head = hashCode;
                current.setBranch(bName);
                current.setHashCode(hashCode);
                Utils.writeObject(Utils.join(commitfolder, hashCode), current);
                remote.allCommitId.add(hashCode);
                ArrayList<String> thisBranch = branchMap.get(bName);
                thisBranch.add(hashCode);
            }
            Utils.writeObject(Utils.join(remotePath,
                    "gitlet"), remote);
        } else {
            System.out.println("Remote directory not found.");
            return;
        }
    }


    public void pull(String name, String bName) {
        fetch(name, bName);
        merge(name + File.separator + bName);

    }

    /** HashMap to save the remote repository.*/
    private HashMap<String, String> remoteRepository;

    /** ArrayList to save all the confilect file.*/
    private ArrayList<String> conflictFile;

    /** the longest untracked error content.*/
    private final String unTrackedError = "There is an untracked file in the way; "
            + "delete it, or add and commit it first.";

    /** the error content.*/
    private String errorTwo = "Given branch is an "
           + "ancestor of the current branch.";

}
