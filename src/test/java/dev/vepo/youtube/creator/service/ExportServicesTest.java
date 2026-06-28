package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.ProjectFixtures;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ExportServicesTest {

    @Inject
    EdlExportService edlExportService;

    @Inject
    OtioExportService otioExportService;

    @Inject
    Projects projects;

    private String projectId;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        var base = Given.persistProject();
        var fixture = ProjectFixtures.withVideoClip();
        base.setName(fixture.getName());
        base.setMedias(fixture.getMedias());
        base.setClips(fixture.getClips());
        base.ensureTracks();
        projects.update(base);
        projectId = base.getId().toHexString();
    }

    @Test
    void edlExportContainsClipEvent() throws Exception {
        var path = edlExportService.export(projectId);
        var content = Files.readString(path);

        assertTrue(content.contains("TITLE: Fixture project"));
        assertTrue(content.contains("FROM CLIP NAME: Opening scene"));
        assertTrue(content.contains("media-ha"));
    }

    @Test
    void otioExportContainsClipJson() throws Exception {
        var path = otioExportService.export(projectId);
        var content = Files.readString(path);

        assertTrue(content.contains("\"OTIO_SCHEMA\": \"Timeline.1\""));
        assertTrue(content.contains("\"name\": \"Opening scene\""));
        assertTrue(content.contains("\"trackIndex\": 0"));
    }

    @Test
    void exportMissingProjectThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> edlExportService.export("000000000000000000000000"));
    }
}
