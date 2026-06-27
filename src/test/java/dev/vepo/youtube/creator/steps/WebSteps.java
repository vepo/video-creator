package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

public class WebSteps {

    @Inject
    ScenarioContext context;

    @Inject
    Projects projects;

    @When("I open the home page")
    public void openHomePage() {
        context.setLastResponse(RestAssured.given().when().get("/"));
    }

    @When("I open the editor for a new project")
    public void openEditorForNewProject() {
        context.setLastResponse(
                RestAssured.given()
                        .redirects().follow(false)
                        .when()
                        .get("/editor/new"));
    }

    @When("I open the editor for the persisted project")
    public void openEditorForPersistedProject() {
        context.setLastResponse(
                RestAssured.given().when().get("/editor/" + context.getProjectId()));
    }

    @When("I open the editor for project {string}")
    public void openEditorForProject(String projectId) {
        context.setLastResponse(
                RestAssured.given()
                        .redirects().follow(false)
                        .when()
                        .get("/editor/" + projectId));
    }

    @When("I upload the test image to the project")
    public void uploadTestImage() {
        var fixture = new File("src/test/resources/fixtures/test.png");
        context.setLastResponse(
                RestAssured.given()
                        .multiPart("file", fixture, "image/png")
                        .formParam("name", "test.png")
                        .when()
                        .post("/api/editor/" + context.getProjectId() + "/media"));
    }

    @When("I download file {string}")
    public void downloadFile(String filename) {
        context.setLastResponse(
                RestAssured.given().when().get("/download/" + filename));
    }

    @When("I request a preview for the timeline project")
    public void requestPreview() {
        context.setLastResponse(
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(context.getTimelineProject())
                        .when()
                        .post("/api/timeline/preview"));
    }

    @When("I request a render for the timeline project")
    public void requestRender() {
        context.setLastResponse(
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(context.getTimelineProject())
                        .when()
                        .post("/api/timeline/render"));
    }

    @Then("the page body should contain {string}")
    public void pageBodyShouldContain(String text) {
        assertTrue(context.getLastResponse().body().asString().contains(text));
    }

    @Then("the location header should point to a project editor URL")
    public void locationShouldPointToEditor() {
        var location = context.getLastResponse().header("Location");
        assertNotNull(location);
        assertTrue(location.contains("/editor/"));
    }

    @Then("the location header should point to the home page")
    public void locationShouldPointToHome() {
        var location = context.getLastResponse().header("Location");
        assertNotNull(location);
        assertTrue(location.equals("/") || location.endsWith("/"),
                "Expected home redirect but was: " + location);
    }

    @Then("the upload response status should be {int}")
    public void uploadResponseStatusShouldBe(int status) {
        assertEquals(status, context.getLastResponse().statusCode());
    }

    @Then("the persisted project should contain the uploaded media")
    public void persistedProjectShouldContainMedia() {
        var project = projects.find(context.getProjectId()).orElseThrow();
        assertTrue(project.getMedias().size() >= 1);
    }

    @Then("the preview response status should be {int} or {int}")
    public void previewResponseStatus(int ok, int error) {
        var status = context.getLastResponse().statusCode();
        assertTrue(status == ok || status == error,
                "Expected " + ok + " or " + error + " but was " + status);
    }

    @Then("the render response status should be {int} or {int}")
    public void renderResponseStatus(int ok, int error) {
        var status = context.getLastResponse().statusCode();
        assertTrue(status == ok || status == error,
                "Expected " + ok + " or " + error + " but was " + status);
    }
}
