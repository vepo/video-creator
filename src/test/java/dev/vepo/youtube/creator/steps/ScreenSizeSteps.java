package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vepo.youtube.creator.project.ScreenSize;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class ScreenSizeSteps {

    @Inject
    ScenarioContext context;

    @Given("the default screen size")
    public void theDefaultScreenSize() {
        context.setScreenSize(ScreenSize.getDefault());
    }

    @Given("the resolution {string}")
    public void theResolution(String resolution) {
        context.setResolutionLookup(resolution);
    }

    @Given("an unknown resolution {string}")
    public void anUnknownResolution(String resolution) {
        context.setResolutionLookup(resolution);
    }

    @Given("the screen size {string}")
    public void theScreenSize(String enumName) {
        context.setScreenSize(ScreenSize.valueOf(enumName));
    }

    @When("I look up the screen size by resolution")
    public void lookUpScreenSizeByResolution() {
        try {
            context.setScreenSize(ScreenSize.fromResolution(context.getResolutionLookup()));
            context.setCaughtException(null);
        } catch (IllegalArgumentException e) {
            context.setCaughtException(e);
        }
    }

    @Then("the screen resolution should be {string}")
    public void screenResolutionShouldBe(String resolution) {
        assertEquals(resolution, context.getScreenSize().getResolution());
    }

    @Then("the screen size should be landscape orientation")
    public void screenSizeShouldBeLandscape() {
        assertTrue(context.getScreenSize().isLandscape());
    }

    @Then("the screen size should be vertical orientation")
    public void screenSizeShouldBeVertical() {
        assertTrue(context.getScreenSize().isVertical());
    }

    @Then("the screen size should be square orientation")
    public void screenSizeShouldBeSquare() {
        assertTrue(context.getScreenSize().isSquare());
    }

    @Then("the screen size name should contain {string}")
    public void screenSizeNameShouldContain(String fragment) {
        assertNotNull(context.getScreenSize());
        assertTrue(context.getScreenSize().getName().contains(fragment));
    }

    @Then("an illegal argument exception should be thrown")
    public void illegalArgumentExceptionShouldBeThrown() {
        assertNotNull(context.getCaughtException());
        assertTrue(context.getCaughtException() instanceof IllegalArgumentException);
    }
}
