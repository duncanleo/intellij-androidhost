package model;

import java.io.File;

/**
 * Created by duncan on 29/9/15.
 */
public class APK {
    private String path;

    public APK(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return new File(path).getName();
    }

    public String getPath() {
        return path;
    }
}
