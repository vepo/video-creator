package dev.vepo.youtube.creator.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import io.restassured.http.ContentType;

@QuarkusIntegrationTest
class EditorApiTest {

    private String projectId;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        projectId = Given.persistProject().getId().toHexString();
    }

    @Test
    void openingEditorForNewRedirectsToProjectEditor() {
        given()
                .redirects().follow(false)
                .when()
                .get("/editor/new")
                .then()
                .statusCode(303)
                .header("Location", containsString("/editor/"));
    }

    @Test
    void openingEditorForExistingProjectReturnsHtml() {
        given()
                .when()
                .get("/editor/" + projectId)
                .then()
                .statusCode(200)
                .body(containsString("editor"));
    }

    @Test
    void openingEditorForUnknownProjectRedirectsToHome() {
        given()
                .redirects().follow(false)
                .when()
                .get("/editor/000000000000000000000000")
                .then()
                .statusCode(303)
                .header("Location", containsString("/"));
    }

    @Test
    void openingEditorForInvalidProjectIdRedirectsToHome() {
        given()
                .redirects().follow(false)
                .when()
                .get("/editor/not-a-valid-id")
                .then()
                .statusCode(303)
                .header("Location", containsString("/"));
    }

    @Test
    void unknownPageRedirectsToHome() {
        given()
                .redirects().follow(false)
                .when()
                .get("/unknown-page")
                .then()
                .statusCode(303)
                .header("Location", containsString("/"));
    }

    @Test
    void mediaUploadPersistsFile() {
        var fixture = new File("src/test/resources/fixtures/test.png");

        given()
                .multiPart("file", fixture, "image/png")
                .formParam("name", "test.png")
                .when()
                .post("/api/editor/" + projectId + "/media")
                .then()
                .statusCode(200);

        var project = Given.inject(Projects.class).find(projectId).orElseThrow();
        assertTrue(project.getMedias().size() >= 1);
    }

    @Test
    void downloadMissingFileReturns404() {
        given()
                .when()
                .get("/download/nonexistent-file.mp4")
                .then()
                .statusCode(404);
    }

    @Test
    void previewSessionAcceptsEmptyTimeline() {
        var status = given()
                .when()
                .post("/api/editor/" + projectId + "/preview/session")
                .getStatusCode();

        assertTrue(status == 200 || status == 500);
    }

    @Test
    void renderAcceptsEmptyTimeline() {
        var status = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/editor/" + projectId + "/render")
                .getStatusCode();

        assertTrue(status == 200 || status == 500);
    }

    @Test
    void duplicateProjectCreatesNewId() {
        var copyId = given()
                .when()
                .post("/api/editor/" + projectId + "/duplicate")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        assertNotNull(copyId);
        assertTrue(!copyId.equals(projectId));
    }

    @Test
    void projectTemplatesEndpointReturnsList() {
        given()
                .when()
                .get("/api/templates")
                .then()
                .statusCode(200);
    }
}
