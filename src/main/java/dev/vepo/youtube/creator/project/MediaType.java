package dev.vepo.youtube.creator.project;

public enum MediaType {
    VIDEO, AUDIO, UNKNOWN;

    public static MediaType load(String mimeType) {
        return switch(mimeType) {
            case String s when s.startsWith("video/") -> VIDEO;
            case String s when s.startsWith("audio/") -> AUDIO;
            default -> UNKNOWN;
        };
    }
}
