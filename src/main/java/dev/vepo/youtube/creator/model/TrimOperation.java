package dev.vepo.youtube.creator.model;

public class TrimOperation {
    private double startTime; // in seconds
    private double endTime;   // in seconds
    
    public TrimOperation() {}
    
    public TrimOperation(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getters and setters
    public double getStartTime() { return startTime; }
    public void setStartTime(double startTime) { this.startTime = startTime; }
    
    public double getEndTime() { return endTime; }
    public void setEndTime(double endTime) { this.endTime = endTime; }
}