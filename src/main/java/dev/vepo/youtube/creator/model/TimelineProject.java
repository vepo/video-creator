package dev.vepo.youtube.creator.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineProject {
    private String id;
    private String name;
    private List<MediaTrack> tracks = new ArrayList<>();
    private VideoSettings videoSettings;
    private double duration = 0.0;
    private double currentTime = 0.0;
    private boolean playing = false;
    
    public TimelineProject() {
        this.videoSettings = new VideoSettings();
        initializeDefaultTracks();
    }
    
    public TimelineProject(String id, String name) {
        this.id = id;
        this.name = name;
        this.videoSettings = new VideoSettings();
        initializeDefaultTracks();
    }
    
    private void initializeDefaultTracks() {
        // Add default video track
        tracks.add(new MediaTrack(0, "Video Track 1", "video"));
        // Add default audio track
        tracks.add(new MediaTrack(1, "Audio Track 1", "audio"));
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<MediaTrack> getTracks() { return tracks; }
    public void setTracks(List<MediaTrack> tracks) { this.tracks = tracks; }
    
    public VideoSettings getVideoSettings() { return videoSettings; }
    public void setVideoSettings(VideoSettings videoSettings) { this.videoSettings = videoSettings; }
    
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    
    public double getCurrentTime() { return currentTime; }
    public void setCurrentTime(double currentTime) { this.currentTime = currentTime; }
    
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    
    // Helper methods
    public MediaTrack getTrack(int index) {
        return tracks.stream()
            .filter(track -> track.getIndex() == index)
            .findFirst()
            .orElse(null);
    }
    
    public MediaTrack getVideoTrack() {
        return tracks.stream()
            .filter(track -> "video".equals(track.getType()))
            .findFirst()
            .orElse(null);
    }
    
    public List<MediaTrack> getVideoTracks() {
        return tracks.stream()
            .filter(track -> "video".equals(track.getType()))
            .toList();
    }
    
    public List<MediaTrack> getAudioTracks() {
        return tracks.stream()
            .filter(track -> "audio".equals(track.getType()))
            .toList();
    }
    
    public void addTrack(MediaTrack track) {
        track.setIndex(tracks.size());
        tracks.add(track);
    }
    
    public void removeTrack(int trackIndex) {
        tracks.removeIf(track -> track.getIndex() == trackIndex);
        // Reindex remaining tracks
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).setIndex(i);
        }
    }
    
    public double calculateTotalDuration() {
        return tracks.stream()
            .mapToDouble(MediaTrack::getTotalDuration)
            .max()
            .orElse(0.0);
    }
    
    public void updateDuration() {
        this.duration = calculateTotalDuration();
    }
    
    public boolean hasContent() {
        return tracks.stream().anyMatch(MediaTrack::hasClips);
    }
}
