package dev.vepo.youtube.creator.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class PreviewSessionApiTest {

    private String projectId;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        projectId = Given.persistProject().getId().toHexString();
    }

    @Test
    void startPreviewSessionReturnsRenderingStatus() {
        given()
                .when()
                .post("/api/editor/" + projectId + "/preview/session")
                .then()
                .statusCode(200)
                .body("sessionId", notNullValue())
                .body("status", equalTo("rendering"))
                .body("percent", equalTo(0));
    }

    @Test
    void getPreviewSessionStatusForUnknownSessionReturns404() {
        given()
                .when()
                .get("/api/editor/" + projectId + "/preview/session/unknownsessionid")
                .then()
                .statusCode(404);
    }

    @Test
    void pollPreviewSessionStatusAfterStart() {
        var sessionId = given()
                .when()
                .post("/api/editor/" + projectId + "/preview/session")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("sessionId");

        var status = given()
                .when()
                .get("/api/editor/" + projectId + "/preview/session/" + sessionId)
                .then()
                .statusCode(200)
                .body("status", anyOf(equalTo("rendering"), equalTo("ready"), equalTo("failed")))
                .extract()
                .jsonPath()
                .getString("status");

        assertEquals(sessionId, given()
                .when()
                .get("/api/editor/" + projectId + "/preview/session/" + sessionId)
                .then()
                .extract()
                .jsonPath()
                .getString("sessionId"));
    }

    @Test
    void stopPreviewSessionReturnsOk() {
        var sessionId = given()
                .post("/api/editor/" + projectId + "/preview/session")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("sessionId");

        given()
                .when()
                .delete("/api/editor/" + projectId + "/preview/session/" + sessionId)
                .then()
                .statusCode(200);
    }
}
