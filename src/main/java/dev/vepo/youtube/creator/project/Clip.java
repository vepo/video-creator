package dev.vepo.youtube.creator.project;

public class Clip {
    private String hash;
    private String name;
    private String mediaHash;
    private long duration;
    private long sourceIn;
    private long sourceOut;
    private MediaType type;
    private long start;
    private double speed = 1.0;
    private String syncGroup;
    private int trackIndex;
    private String transition;
    private String effect;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMediaHash() {
        return mediaHash;
    }

    public void setMediaHash(String mediaHash) {
        this.mediaHash = mediaHash;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSourceIn() {
        return sourceIn;
    }

    public void setSourceIn(long sourceIn) {
        this.sourceIn = sourceIn;
    }

    public long getSourceOut() {
        return sourceOut;
    }

    public void setSourceOut(long sourceOut) {
        this.sourceOut = sourceOut;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getSyncGroup() {
        return syncGroup;
    }

    public void setSyncGroup(String syncGroup) {
        this.syncGroup = syncGroup;
    }

    public int getTrackIndex() {
        return trackIndex;
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }
}
