package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProjectArchiveService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectArchiveService.class);

    @Inject
    Projects projects;

    @Inject
    MediaService mediaService;

    public Path createArchive(String projectId) throws IOException {
        Project project = projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Path archiveDir = mediaService.getOutputPath("archives").getParent().resolve("archives");
        Files.createDirectories(archiveDir);
        Path archivePath = archiveDir.resolve("project_" + projectId + ".zip");
        try (OutputStream out = Files.newOutputStream(archivePath);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("project.json"));
            zip.write(project.asJson().getBytes());
            zip.closeEntry();
            if (project.getMedias() != null) {
                for (var media : project.getMedias()) {
                    try {
                        Path materialized = mediaService.materializeMedia(media);
                        zip.putNextEntry(new ZipEntry("media/" + media.getHash() + "_" + media.getName()));
                        Files.copy(materialized, zip);
                        zip.closeEntry();
                    } catch (IOException e) {
                        logger.warn("Skipping media {} in archive: {}", media.getHash(), e.getMessage());
                    }
                }
            }
        }
        return archivePath;
    }
}
