package dev.vepo.youtube.creator.project;

public class ClipTransition {
    private String type;
    private long durationMs;
    private String alignment;

    public ClipTransition() {
    }

    public ClipTransition(String type, long durationMs, String alignment) {
        this.type = type;
        this.durationMs = durationMs;
        this.alignment = alignment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }
}
