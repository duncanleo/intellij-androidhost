package model;

import java.util.Map;

/**
 * Created by duncan on 24/9/15.
 */
public class ADBDevice {
    private String id;
    private String status;
    private Map<String, String> properties;

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
