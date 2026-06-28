package dev.vepo.youtube.creator.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.ProjectFixtures;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ExportApiTest {

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
    void edlExportEndpointReturnsAttachment() {
        given()
                .when()
                .get("/api/editor/" + projectId + "/export/edl")
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString(".edl"));
    }

    @Test
    void otioExportEndpointReturnsAttachment() {
        given()
                .when()
                .get("/api/editor/" + projectId + "/export/otio")
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString(".otio"));
    }
}
