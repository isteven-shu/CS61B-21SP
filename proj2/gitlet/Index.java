package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import static gitlet.Repository.INDEX;
import static gitlet.Utils.writeObject;

public class Index implements Serializable {
    public HashMap<String, String> staged;
    public HashSet<String> removed;

    Index() {
        staged = new HashMap<>();
        removed = new HashSet<>();
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
}
