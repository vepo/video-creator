package dev.vepo.youtube.creator.model;

public class VideoSettings {
    private String videoCodec = "libx264";
    private String audioCodec = "aac";
    private Integer crf = 23;
    private String preset = "medium";
    private Integer width;
    private Integer height;
    private Integer audioBitrate = 128;
    
    public VideoSettings() {}
    
    // Getters and setters
    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
    
    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
    
    public Integer getCrf() { return crf; }
    public void setCrf(Integer crf) { this.crf = crf; }
    
    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public Integer getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(Integer audioBitrate) { this.audioBitrate = audioBitrate; }
}