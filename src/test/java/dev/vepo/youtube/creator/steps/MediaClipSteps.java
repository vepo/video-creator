package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class MediaClipSteps {

    @Inject
    ScenarioContext context;

    private MediaClip clip;

    @Given("a clip from {double} to {double} seconds at speed {double}")
    public void aClipWithSpeed(double start, double end, double speed) {
        clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(start);
        clip.setEndTime(end);
        clip.setSpeed(speed);
    }

    @Given("a clip with id, file path, file name, and type")
    public void aValidClip() {
        clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
    }

    @When("the clip effective duration is calculated")
    public void clipEffectiveDurationIsCalculated() {
        context.setClipEffectiveDuration(clip.getEffectiveDuration());
    }

    @Then("the clip effective duration should be {double} seconds")
    public void clipEffectiveDurationShouldBe(double expected) {
        assertEquals(expected, context.getClipEffectiveDuration(), 0.001);
    }

    @Then("the clip should be valid")
    public void clipShouldBeValid() {
        assertTrue(clip.isValid());
    }
}
