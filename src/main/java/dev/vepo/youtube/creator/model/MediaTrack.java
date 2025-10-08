package dev.vepo.youtube.creator.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaTrack {
    private int index;
    private String name;
    private String type; // "video" or "audio"
    private boolean visible = true;
    private boolean locked = false;
    private double volume = 1.0;
    private List<MediaClip> clips = new ArrayList<>();
    
    public MediaTrack() {}
    
    public MediaTrack(int index, String name, String type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }
    
    // Getters and setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    
    public List<MediaClip> getClips() { return clips; }
    public void setClips(List<MediaClip> clips) { this.clips = clips; }
    
    // Helper methods
    public void addClip(MediaClip clip) {
        clip.setTrackIndex(this.index);
        this.clips.add(clip);
    }
    
    public void removeClip(String clipId) {
        this.clips.removeIf(clip -> clip.getId().equals(clipId));
    }
    
    public MediaClip getClip(String clipId) {
        return this.clips.stream()
            .filter(clip -> clip.getId().equals(clipId))
            .findFirst()
            .orElse(null);
    }
    
    public double getTotalDuration() {
        return this.clips.stream()
            .mapToDouble(clip -> clip.getTimelinePosition() + clip.getEffectiveDuration())
            .max()
            .orElse(0.0);
    }
    
    public boolean hasClips() {
        return !this.clips.isEmpty();
    }
}
