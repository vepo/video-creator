package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.types.ObjectId;

import dev.vepo.youtube.creator.project.FrameRate;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.ScreenSize;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class ProjectSteps {

    @Inject
    ScenarioContext context;

    @Given("a newly created project")
    public void aNewlyCreatedProject() {
        context.setProject(new Project());
    }

    @Given("a project with a known id")
    public void aProjectWithKnownId() {
        var project = new Project();
        project.setId(new ObjectId());
        context.setProject(project);
    }

    @Given("another project with the same id")
    public void anotherProjectWithSameId() {
        var project = new Project();
        project.setId(context.getProject().getId());
        context.setSecondProject(project);
    }

    @Then("the project screen size should be Full HD 1080p")
    public void projectScreenSizeShouldBeFullHd() {
        assertEquals(ScreenSize.HD_1080p, context.getProject().getScreenSize());
    }

    @Then("the project frame rate should be 30 FPS web streaming")
    public void projectFrameRateShouldBeDefault() {
        assertEquals(FrameRate.FPS_30_WEB, context.getProject().getFrameRate());
    }

    @Then("the project duration should be 30 minutes in milliseconds")
    public void projectDurationShouldBeThirtyMinutes() {
        assertEquals(1000L * 60 * 30, context.getProject().getDuration());
    }

    @Then("the project should have no media attached")
    public void projectShouldHaveNoMedia() {
        assertTrue(context.getProject().getMedias().isEmpty());
    }

    @Then("the project should have no clips attached")
    public void projectShouldHaveNoClips() {
        assertTrue(context.getProject().getClips().isEmpty());
    }

    @Then("the project name should start with {string}")
    public void projectNameShouldStartWith(String prefix) {
        assertTrue(context.getProject().getName().startsWith(prefix));
    }

    @Then("the projects should be equal")
    public void projectsShouldBeEqual() {
        assertEquals(context.getProject(), context.getSecondProject());
    }

    @When("the project is serialized to JSON")
    public void projectIsSerializedToJson() throws Exception {
        context.setStringResult(context.getProject().asJson());
    }

    @Then("the JSON should contain the project name")
    public void jsonShouldContainProjectName() {
        assertNotNull(context.getStringResult());
        assertTrue(context.getStringResult().contains(context.getProject().getName()));
    }
}
