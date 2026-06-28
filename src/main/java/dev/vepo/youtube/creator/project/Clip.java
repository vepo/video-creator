package dev.vepo.youtube.creator.project;

import java.util.ArrayList;
import java.util.List;

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
    private List<ClipEffect> effects = new ArrayList<>();
    private ClipTransition transition;
    private List<Keyframe> volumeKeyframes = new ArrayList<>();
    private double volume = 100.0;
    private ColorGrade colorGrade;
    private TitleClip title;

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

    public List<ClipEffect> getEffects() {
        return effects;
    }

    public void setEffects(List<ClipEffect> effects) {
        this.effects = effects != null ? effects : new ArrayList<>();
    }

    public ClipTransition getTransition() {
        return transition;
    }

    public void setTransition(ClipTransition transition) {
        this.transition = transition;
    }

    public List<Keyframe> getVolumeKeyframes() {
        return volumeKeyframes;
    }

    public void setVolumeKeyframes(List<Keyframe> volumeKeyframes) {
        this.volumeKeyframes = volumeKeyframes != null ? volumeKeyframes : new ArrayList<>();
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public ColorGrade getColorGrade() {
        return colorGrade;
    }

    public void setColorGrade(ColorGrade colorGrade) {
        this.colorGrade = colorGrade;
    }

    public TitleClip getTitle() {
        return title;
    }

    public void setTitle(TitleClip title) {
        this.title = title;
    }
}
