package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class MediaTrackSteps {

    @Inject
    ScenarioContext context;

    private MediaTrack track;

    @Given("a media track at index {int}")
    public void aMediaTrackAtIndex(int index) {
        track = new MediaTrack(index, "Track " + index, "video");
    }

    @Given("a clip on the track from {double} to {double} seconds at timeline position {double}")
    public void clipOnTrack(double start, double end, double position) {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(start);
        clip.setEndTime(end);
        clip.setTimelinePosition(position);
        track.addClip(clip);
    }

    @When("a clip is added to the track")
    public void clipIsAddedToTrack() {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        track.addClip(clip);
        context.setStringResult(String.valueOf(clip.getTrackIndex()));
    }

    @When("the track total duration is calculated")
    public void trackTotalDurationIsCalculated() {
        context.setTrackTotalDuration(track.getTotalDuration());
    }

    @Then("the clip track index should be {int}")
    public void clipTrackIndexShouldBe(int index) {
        assertEquals(index, Integer.parseInt(context.getStringResult()));
    }

    @Then("the track total duration should be {double} seconds")
    public void trackTotalDurationShouldBe(double expected) {
        assertEquals(expected, context.getTrackTotalDuration(), 0.001);
    }

    @Then("the track should not have clips")
    public void trackShouldNotHaveClips() {
        assertFalse(track.hasClips());
    }
}
