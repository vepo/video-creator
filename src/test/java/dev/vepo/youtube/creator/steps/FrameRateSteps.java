package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vepo.youtube.creator.project.FrameRate;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class FrameRateSteps {

    @Inject
    ScenarioContext context;

    @Given("the default frame rate")
    public void theDefaultFrameRate() {
        context.setFrameRate(FrameRate.getDefault());
    }

    @Given("the frame rate value string {string}")
    public void theFrameRateValueString(String value) {
        context.setStringResult(value);
    }

    @Given("the frame rate {string}")
    public void theFrameRate(String enumName) {
        context.setFrameRate(FrameRate.valueOf(enumName));
    }

    @When("I look up the frame rate by value")
    public void lookUpFrameRateByValue() {
        context.setFrameRate(FrameRate.fromValueString(context.getStringResult()));
    }

    @Then("the frame rate value should be {double}")
    public void frameRateValueShouldBe(double value) {
        assertEquals(value, context.getFrameRate().getValue());
    }

    @Then("the frame rate category should be {string}")
    public void frameRateCategoryShouldBe(String category) {
        assertEquals(category, context.getFrameRate().getCategory());
    }

    @Then("the frame rate should be film cinema category")
    public void frameRateShouldBeFilmCinema() {
        assertTrue(context.getFrameRate().isFilmCinema());
    }

    @Then("the frame rate should support slow motion")
    public void frameRateShouldSupportSlowMotion() {
        assertTrue(context.getFrameRate().isSlowMotionCapable());
    }
}
