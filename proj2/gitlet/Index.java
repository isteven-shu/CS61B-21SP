package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class Index implements Serializable {
    public HashMap<String, String> staged;
    public HashMap<String, String> removed;

    Index() {
        staged = new HashMap<>();
        removed = new HashMap<>();
    }
}
