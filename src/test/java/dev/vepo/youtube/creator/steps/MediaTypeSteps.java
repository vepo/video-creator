package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vepo.youtube.creator.project.MediaType;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class MediaTypeSteps {

    @Inject
    ScenarioContext context;

    @Given("the mime type {string}")
    public void theMimeType(String mime) {
        context.setMimeType(mime);
    }

    @When("the media type is resolved")
    public void mediaTypeIsResolved() {
        context.setMediaType(MediaType.load(context.getMimeType()));
    }

    @Then("the media type should be {string}")
    public void mediaTypeShouldBe(String expected) {
        assertEquals(MediaType.valueOf(expected), context.getMediaType());
    }
}
