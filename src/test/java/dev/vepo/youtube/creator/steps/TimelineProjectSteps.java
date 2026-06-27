package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class TimelineProjectSteps {

    @Inject
    ScenarioContext context;

    @Given("a new timeline project")
    public void aNewTimelineProject() {
        context.setTimelineProject(new TimelineProject());
    }

    @Given("the video track has a clip from {double} to {double} seconds at timeline position {double}")
    public void videoTrackHasClip(double start, double end, double position) {
        var track = context.getTimelineProject().getVideoTrack();
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(start);
        clip.setEndTime(end);
        clip.setTimelinePosition(position);
        track.addClip(clip);
    }

    @When("the timeline duration is recalculated")
    public void timelineDurationIsRecalculated() {
        context.getTimelineProject().updateDuration();
    }

    @Then("the timeline should have {int} tracks")
    public void timelineShouldHaveTracks(int count) {
        assertEquals(count, context.getTimelineProject().getTracks().size());
    }

    @Then("the timeline should have a video track")
    public void timelineShouldHaveVideoTrack() {
        assertNotNull(context.getTimelineProject().getVideoTrack());
    }

    @Then("the timeline should have an audio track")
    public void timelineShouldHaveAudioTrack() {
        assertFalse(context.getTimelineProject().getAudioTracks().isEmpty());
    }

    @Then("the timeline should not have content")
    public void timelineShouldNotHaveContent() {
        assertFalse(context.getTimelineProject().hasContent());
    }

    @Then("the timeline duration should be greater than {int}")
    public void timelineDurationShouldBeGreaterThan(int min) {
        assertTrue(context.getTimelineProject().getDuration() > min);
    }
}
