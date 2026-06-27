package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class ProjectsPersistenceSteps {

    @Inject
    ScenarioContext context;

    @Inject
    Projects projects;

    @Given("a new project is persisted")
    public void aNewProjectIsPersisted() {
        context.setProject(projects.newProject());
        context.setProjectId(context.getProject().getId().toHexString());
    }

    @When("the project is loaded by id")
    public void projectIsLoadedById() {
        context.setSecondProject(
                projects.find(context.getProjectId()).orElseThrow());
    }

    @When("all projects are loaded")
    public void allProjectsAreLoaded() {
        context.setStringResult(String.valueOf(projects.loadAll().size()));
    }

    @Then("the loaded project name should match the persisted project")
    public void loadedProjectNameShouldMatch() {
        assertEquals(context.getProject().getName(), context.getSecondProject().getName());
    }

    @Then("the project list should contain the persisted project")
    public void projectListShouldContainPersistedProject() {
        var contains = projects.loadAll().stream()
                .map(Project::getId)
                .anyMatch(id -> id.equals(context.getProject().getId()));
        assertTrue(contains);
    }
}
