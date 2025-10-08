package dev.vepo.youtube.creator.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.MediaClip;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MLTXmlGenerator {
    
    public String generateMLTXml(String inputVideoPath, List<TrimOperation> trimOperations, 
                               VideoSettings settings, String outputPath) throws IOException {
        Path xmlPath = Files.createTempFile("mlt_project_", ".xml");
        
        try (BufferedWriter writer = Files.newBufferedWriter(xmlPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<mlt>\n");
            
            // Profile - use proper frame rate and dimensions
            if (settings.getWidth() != null && settings.getHeight() != null) {
                writer.write(String.format("  <profile width=\"%d\" height=\"%d\" frame_rate_num=\"60\" frame_rate_den=\"1\" progressive=\"1\"/>\n", 
                    settings.getWidth(), settings.getHeight()));
            } else {
                writer.write("  <profile width=\"1920\" height=\"1080\" frame_rate_num=\"60\" frame_rate_den=\"1\" progressive=\"1\"/>\n");
            }
            
            // Producer for input video
            writer.write(String.format("  <producer id=\"producer0\">\n"));
            writer.write(String.format("    <property name=\"resource\">%s</property>\n", 
                java.nio.file.Paths.get(inputVideoPath).toAbsolutePath().toString()));
            writer.write("  </producer>\n");
            
            // Playlist with trim operations
            writer.write("  <playlist id=\"playlist0\">\n");
            
            if (trimOperations != null && !trimOperations.isEmpty()) {
                for (int i = 0; i < trimOperations.size(); i++) {
                    TrimOperation trim = trimOperations.get(i);
                    // Use 60 fps for frame calculation (from video properties)
                    int startFrame = (int) (trim.getStartTime() * 60);
                    int endFrame = (int) (trim.getEndTime() * 60);
                    writer.write(String.format("    <entry producer=\"producer0\" in=\"%d\" out=\"%d\"/>\n", 
                        startFrame, endFrame));
                }
            } else {
                // Use entire video if no trim operations
                writer.write("    <entry producer=\"producer0\"/>\n");
            }
            
            writer.write("  </playlist>\n");
            
            // Tractor
            writer.write("  <tractor id=\"tractor0\">\n");
            writer.write("    <track producer=\"playlist0\"/>\n");
            writer.write("  </tractor>\n");
            
            writer.write("</mlt>");
        }
        
        return xmlPath.toString();
    }
    
    public String generateTimelineMLTXml(TimelineProject project) throws IOException {
        Path xmlPath = Files.createTempFile("timeline_project_", ".xml");
        
        try (BufferedWriter writer = Files.newBufferedWriter(xmlPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<mlt>\n");
            
            // Profile
            VideoSettings settings = project.getVideoSettings();
            if (settings.getWidth() != null && settings.getHeight() != null) {
                writer.write(String.format("  <profile width=\"%d\" height=\"%d\" frame_rate_num=\"60\" frame_rate_den=\"1\" progressive=\"1\"/>\n", 
                    settings.getWidth(), settings.getHeight()));
            } else {
                writer.write("  <profile width=\"1920\" height=\"1080\" frame_rate_num=\"60\" frame_rate_den=\"1\" progressive=\"1\"/>\n");
            }
            
            int producerId = 0;
            int playlistId = 0;
            
            // Generate producers for all media files
            for (MediaTrack track : project.getTracks()) {
                for (MediaClip clip : track.getClips()) {
                    writer.write(String.format("  <producer id=\"producer%d\">\n", producerId));
                    writer.write(String.format("    <property name=\"resource\">%s</property>\n", 
                        java.nio.file.Paths.get(clip.getFilePath()).toAbsolutePath().toString()));
                    writer.write("  </producer>\n");
                    clip.setId("producer" + producerId); // Store producer ID in clip for reference
                    producerId++;
                }
            }
            
            // Generate playlists for each track
            for (MediaTrack track : project.getTracks()) {
                if (!track.hasClips()) continue;
                
                writer.write(String.format("  <playlist id=\"playlist%d\">\n", playlistId));
                
                // Sort clips by timeline position
                List<MediaClip> sortedClips = track.getClips().stream()
                    .sorted((a, b) -> Double.compare(a.getTimelinePosition(), b.getTimelinePosition()))
                    .toList();
                
                for (MediaClip clip : sortedClips) {
                    double speed = clip.getSpeed() > 0 ? clip.getSpeed() : 1.0;
                    int startFrame = (int) (clip.getStartTime() * 60);
                    int endFrame = (int) (clip.getEndTime() * 60);
                    int timelineFrame = (int) (clip.getTimelinePosition() * 60);
                    int effectiveEndFrame = (int) (timelineFrame + ((endFrame - startFrame) / speed));

                    // Always use start/end for clarity, and add speed property if not 1.0
                    writer.write(String.format("    <entry producer=\"%s\" in=\"%d\" out=\"%d\" position=\"%d\"/>", 
                        clip.getId(), startFrame, endFrame, timelineFrame));
                    if (speed != 1.0) {
                        writer.write(String.format("    <property name=\"speed\">%.2f</property>\n", speed));
                    }
                }
                
                writer.write("  </playlist>\n");
                playlistId++;
            }
            
            // Generate tractor with all playlists
            writer.write("  <tractor id=\"tractor0\">\n");
            playlistId = 0;
            for (MediaTrack track : project.getTracks()) {
                if (track.hasClips()) {
                    writer.write(String.format("    <track producer=\"playlist%d\"/>\n", playlistId));
                    playlistId++;
                }
            }
            writer.write("  </tractor>\n");
            
            writer.write("</mlt>");
        }
        
        return xmlPath.toString();
    }
}