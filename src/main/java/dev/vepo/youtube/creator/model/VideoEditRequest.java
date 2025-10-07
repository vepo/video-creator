package dev.vepo.youtube.creator.model;

import java.util.List;

public class VideoEditRequest {
    private String inputFile;
    private String outputFile;
    private List<TrimOperation> trimOperations;
    private VideoSettings videoSettings;
    
    // Constructors, getters, and setters
    public VideoEditRequest() {}
    
    public VideoEditRequest(String inputFile, String outputFile, List<TrimOperation> trimOperations, VideoSettings videoSettings) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.trimOperations = trimOperations;
        this.videoSettings = videoSettings;
    }
    
    // Getters and setters...
    public String getInputFile() { return inputFile; }
    public void setInputFile(String inputFile) { this.inputFile = inputFile; }
    
    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String outputFile) { this.outputFile = outputFile; }
    
    public List<TrimOperation> getTrimOperations() { return trimOperations; }
    public void setTrimOperations(List<TrimOperation> trimOperations) { this.trimOperations = trimOperations; }
    
    public VideoSettings getVideoSettings() { return videoSettings; }
    public void setVideoSettings(VideoSettings videoSettings) { this.videoSettings = videoSettings; }
}