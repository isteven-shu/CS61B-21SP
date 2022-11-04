# Gitlet Design Document

**Name**: Shuyuan Wang

## Classes and Data Structures

### Repository

#### Fields
--- static ---
Represents the paths
1. CWD
2. GITLET_DIR
3. BRANCHES_DIR
4. OBJECTS_DIR
5. BLOBS_DIR
6. COMMITS_DIR

### Commit
1. Date date
2. String message
3. HashMap<String, String> blobs
   - recording all the blobs in this commit
   - mapping from filename to commitID
4. String parent

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence

gitlet
--HEAD (the content of HEAD is the name of the branch we currently on)
--branches/
--objects/
   --blobs/ (each blob is named by the SHA1 of hte content of the file and its content is the original content)
   --commits/



