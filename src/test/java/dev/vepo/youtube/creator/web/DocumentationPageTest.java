package dev.vepo.youtube.creator.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.service.UserDocumentationService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class DocumentationPageTest {

    @Test
    void docsPageReturnsHtml() {
        RestAssured.given()
                .when()
                .get("/docs")
                .then()
                .statusCode(200)
                .body(containsString("Video Creator"))
                .body(containsString("User Guide"));
    }

    @Test
    void userGuideRendersMarkdownSections() {
        var service = new UserDocumentationService();
        String html = service.renderHtml();
        assertTrue(html.contains("<h2"));
        assertTrue(html.contains("Timeline"));
        assertTrue(html.contains("Keyboard shortcuts"));
    }
}
