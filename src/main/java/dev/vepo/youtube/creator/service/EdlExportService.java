package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EdlExportService {

    @Inject
    Projects projects;

    @Inject
    MediaService mediaService;

    public Path export(String projectId) throws IOException {
        Project project = projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Path exportDir = mediaService.getOutputPath("exports").getParent().resolve("exports");
        Files.createDirectories(exportDir);
        Path output = exportDir.resolve("project_" + projectId + ".edl");
        StringBuilder edl = new StringBuilder();
        edl.append("TITLE: ").append(project.getName()).append("\n");
        edl.append("FCM: NON-DROP FRAME\n\n");
        int eventNum = 1;
        if (project.getClips() != null) {
            for (Clip clip : project.getClips()) {
                Media media = findMedia(project, clip.getMediaHash());
                String reel = media != null ? media.getHash().substring(0, Math.min(8, media.getHash().length()))
                        : "AX";
                double recIn = clip.getStart() / 1000.0;
                double recOut = recIn + clip.getDuration() / 1000.0;
                double srcIn = clip.getSourceIn() / 1000.0;
                double srcOut = clip.getSourceOut() > 0 ? clip.getSourceOut() / 1000.0
                        : srcIn + clip.getDuration() / 1000.0;
                edl.append(String.format(Locale.US, "%03d  %s V     C        %s %s %s %s\n",
                        eventNum++, reel,
                        formatTimecode(srcIn), formatTimecode(srcOut),
                        formatTimecode(recIn), formatTimecode(recOut)));
                edl.append("* FROM CLIP NAME: ").append(clip.getName()).append("\n\n");
            }
        }
        Files.writeString(output, edl.toString());
        return output;
    }

    private Media findMedia(Project project, String mediaHash) {
        if (project.getMedias() == null || mediaHash == null) {
            return null;
        }
        return project.getMedias().stream()
                .filter(m -> mediaHash.equals(m.getHash()))
                .findFirst()
                .orElse(null);
    }

    private String formatTimecode(double seconds) {
        int totalFrames = (int) (seconds * 30);
        int frames = totalFrames % 30;
        int totalSeconds = totalFrames / 30;
        int s = totalSeconds % 60;
        int totalMinutes = totalSeconds / 60;
        int m = totalMinutes % 60;
        int h = totalMinutes / 60;
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", h, m, s, frames);
    }
}
