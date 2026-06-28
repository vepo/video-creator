package dev.vepo.youtube.creator.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaClip {
    private String id;
    private String filePath;
    private String fileName;
    private String type; // "video" or "audio"
    private double startTime; // Start time in the source media (seconds)
    private double endTime;   // End time in the source media (seconds)
    private double timelinePosition; // Position on the timeline (seconds)
    private double duration; // Duration of the clip (seconds)
    private int trackIndex; // Which track this clip belongs to
    private boolean muted;
    private double volume = 1.0;
    private double speed = 1.0; // Playback speed multiplier
    private String effect;
    private String transition;
    private List<MediaClipEffect> effects = new ArrayList<>();
    private MediaClipTransition clipTransition;
    private List<MediaKeyframe> volumeKeyframes = new ArrayList<>();
    
    public MediaClip() {}
    
    public MediaClip(String id, String filePath, String fileName, String type) {
    this.id = id;
    this.filePath = filePath;
    this.fileName = fileName;
    this.type = type;
    this.speed = 1.0;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public double getStartTime() { return startTime; }
    public void setStartTime(double startTime) { this.startTime = startTime; }
    
    public double getEndTime() { return endTime; }
    public void setEndTime(double endTime) { this.endTime = endTime; }
    
    public double getTimelinePosition() { return timelinePosition; }
    public void setTimelinePosition(double timelinePosition) { this.timelinePosition = timelinePosition; }
    
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    
    public int getTrackIndex() { return trackIndex; }
    public void setTrackIndex(int trackIndex) { this.trackIndex = trackIndex; }
    
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public String getTransition() { return transition; }
    public void setTransition(String transition) { this.transition = transition; }

    public List<MediaClipEffect> getEffects() { return effects; }
    public void setEffects(List<MediaClipEffect> effects) {
        this.effects = effects != null ? effects : new ArrayList<>();
    }

    public MediaClipTransition getClipTransition() { return clipTransition; }
    public void setClipTransition(MediaClipTransition clipTransition) { this.clipTransition = clipTransition; }

    public List<MediaKeyframe> getVolumeKeyframes() { return volumeKeyframes; }
    public void setVolumeKeyframes(List<MediaKeyframe> volumeKeyframes) {
        this.volumeKeyframes = volumeKeyframes != null ? volumeKeyframes : new ArrayList<>();
    }
    
    // Helper methods
    public double getEffectiveDuration() {
        if (endTime > 0 && startTime >= 0) {
            double baseDuration = endTime - startTime;
            return baseDuration / (speed > 0 ? speed : 1.0);
        }
        return duration / (speed > 0 ? speed : 1.0);
    }
    
    public boolean isValid() {
        return id != null && filePath != null && fileName != null && type != null;
    }
}
