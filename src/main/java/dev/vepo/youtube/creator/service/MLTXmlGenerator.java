package dev.vepo.youtube.creator.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MLTXmlGenerator {
    
    public String generateMLTXml(String inputVideoPath, List<TrimOperation> trimOperations, 
                               VideoSettings settings, String outputPath) throws IOException {
        Path xmlPath = Files.createTempFile("mlt_project_", ".xml");
        
        try (BufferedWriter writer = Files.newBufferedWriter(xmlPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<mlt>\n");
            
            // Profile
            if (settings.getWidth() != null && settings.getHeight() != null) {
                writer.write(String.format("  <profile width=\"%d\" height=\"%d\"/>\n", 
                    settings.getWidth(), settings.getHeight()));
            } else {
                writer.write("  <profile description=\"automatic\"/>\n");
            }
            
            // Producer for input video
            writer.write(String.format("  <producer id=\"producer0\">\n"));
            writer.write(String.format("    <property name=\"resource\">%s</property>\n", inputVideoPath));
            writer.write("  </producer>\n");
            
            // Playlist with trim operations
            writer.write("  <playlist id=\"playlist0\">\n");
            
            if (trimOperations != null && !trimOperations.isEmpty()) {
                for (int i = 0; i < trimOperations.size(); i++) {
                    TrimOperation trim = trimOperations.get(i);
                    int startFrame = (int) (trim.getStartTime() * 25); // Assuming 25 fps
                    int endFrame = (int) (trim.getEndTime() * 25);
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
}