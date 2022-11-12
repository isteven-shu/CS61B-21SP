package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import static gitlet.Repository.INDEX;
import static gitlet.Utils.*;

public class Index implements Serializable {
    public HashMap<String, String> staged;
    public HashMap<String, String> removed;

    Index() {
        staged = new HashMap<>();
        removed = new HashMap<>();
    }

    public void clear() {
        staged.clear();
        removed.clear();
    }

    public boolean isEmpty() {
        return staged.isEmpty() && removed.isEmpty();
    }

    public void save() {
        writeObject(INDEX, this);
    }

    public static Index getStagingArea() {
        if (INDEX.exists()) {
            return readObject(INDEX, Index.class);
        }
        return new Index();
    }
}
