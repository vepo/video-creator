package dev.vepo.youtube.creator.project;

import java.util.ArrayList;
import java.util.List;

public class MulticamGroup {
    private String id;
    private String name;
    private List<String> clipHashes = new ArrayList<>();
    private int activeAngle;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getClipHashes() {
        return clipHashes;
    }

    public void setClipHashes(List<String> clipHashes) {
        this.clipHashes = clipHashes != null ? clipHashes : new ArrayList<>();
    }

    public int getActiveAngle() {
        return activeAngle;
    }

    public void setActiveAngle(int activeAngle) {
        this.activeAngle = activeAngle;
    }
}
