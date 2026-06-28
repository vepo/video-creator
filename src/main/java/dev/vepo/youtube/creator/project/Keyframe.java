package dev.vepo.youtube.creator.project;

public class Keyframe {
    private long timeMs;
    private double value;

    public Keyframe() {
    }

    public Keyframe(long timeMs, double value) {
        this.timeMs = timeMs;
        this.value = value;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
