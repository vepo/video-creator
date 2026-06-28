package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OtioExportService {

    @Inject
    Projects projects;

    @Inject
    MediaService mediaService;

    public Path export(String projectId) throws IOException {
        Project project = projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Path exportDir = mediaService.ensureOutputSubdir("exports");
        Path output = exportDir.resolve("project_" + project.getId().toHexString() + ".otio");
        StringBuilder otio = new StringBuilder();
        otio.append("{\n");
        otio.append("  \"OTIO_SCHEMA\": \"Timeline.1\",\n");
        otio.append("  \"name\": \"").append(escapeJson(project.getName())).append("\",\n");
        otio.append("  \"tracks\": {\n");
        otio.append("    \"OTIO_SCHEMA\": \"Stack.1\",\n");
        otio.append("    \"children\": [\n");
        if (project.getClips() != null) {
            for (int i = 0; i < project.getClips().size(); i++) {
                Clip clip = project.getClips().get(i);
                if (i > 0) {
                    otio.append(",\n");
                }
                appendClip(otio, clip);
            }
        }
        otio.append("\n    ]\n");
        otio.append("  }\n");
        otio.append("}\n");
        Files.writeString(output, otio.toString());
        return output;
    }

    private void appendClip(StringBuilder otio, Clip clip) {
        otio.append("      {\n");
        otio.append("        \"OTIO_SCHEMA\": \"Clip.1\",\n");
        otio.append("        \"name\": \"").append(escapeJson(clip.getName())).append("\",\n");
        otio.append(String.format(Locale.US,
                "        \"source_range\": { \"start_time\": { \"value\": %d, \"rate\": 1000 }, "
                        + "\"duration\": { \"value\": %d, \"rate\": 1000 } },\n",
                clip.getSourceIn(), clip.getDuration()));
        otio.append("        \"metadata\": { \"trackIndex\": ").append(clip.getTrackIndex()).append(" }\n");
        otio.append("      }");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
