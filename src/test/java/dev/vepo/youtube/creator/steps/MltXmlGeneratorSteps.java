package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.service.MLTXmlGenerator;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

public class MltXmlGeneratorSteps {

    @Inject
    ScenarioContext context;

    @Inject
    MLTXmlGenerator mltXmlGenerator;

    @Given("video settings with width {int} and height {int}")
    public void videoSettingsWithDimensions(int width, int height) {
        var settings = new VideoSettings();
        settings.setWidth(width);
        settings.setHeight(height);
        context.setVideoSettings(settings);
    }

    @Given("an input video at {string}")
    public void anInputVideoAt(String path) throws Exception {
        var videoPath = Path.of(path);
        Files.createDirectories(videoPath.getParent());
        if (!Files.exists(videoPath)) {
            Files.writeString(videoPath, "placeholder");
        }
        context.setInputVideoPath(videoPath.toString());
    }

    @Given("trim operations from {double} to {double} seconds")
    public void trimOperations(double start, double end) {
        context.setTrimOperations(List.of(new TrimOperation(start, end)));
    }

    @Given("a timeline project with a video clip on the video track")
    public void timelineWithVideoClip() throws Exception {
        var project = new TimelineProject();
        var clip = new MediaClip("clip-1", "target/test-input/sample.mp4", "sample.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(5);
        clip.setTimelinePosition(0);
        anInputVideoAt("target/test-input/sample.mp4");
        project.getVideoTrack().addClip(clip);
        context.setTimelineProject(project);
    }

    @When("MLT XML is generated without trim operations")
    public void mltXmlGeneratedWithoutTrims() throws Exception {
        context.setMltXmlPath(mltXmlGenerator.generateMLTXml(
                context.getInputVideoPath(),
                null,
                context.getVideoSettings(),
                "target/test-output/out.mp4"));
        context.setMltXmlContent(Files.readString(Path.of(context.getMltXmlPath())));
    }

    @When("MLT XML is generated with trim operations")
    public void mltXmlGeneratedWithTrims() throws Exception {
        context.setMltXmlPath(mltXmlGenerator.generateMLTXml(
                context.getInputVideoPath(),
                context.getTrimOperations(),
                context.getVideoSettings(),
                "target/test-output/out.mp4"));
        context.setMltXmlContent(Files.readString(Path.of(context.getMltXmlPath())));
    }

    @When("timeline MLT XML is generated")
    public void timelineMltXmlGenerated() throws Exception {
        context.setMltXmlPath(mltXmlGenerator.generateTimelineMLTXml(context.getTimelineProject()));
        context.setMltXmlContent(Files.readString(Path.of(context.getMltXmlPath())));
    }

    @Then("the MLT XML should contain profile width {string}")
    public void mltXmlShouldContainProfileWidth(String width) {
        assertTrue(context.getMltXmlContent().contains("width=\"" + width + "\""));
    }

    @Then("the MLT XML should contain a full-length playlist entry")
    public void mltXmlShouldContainFullLengthEntry() {
        assertTrue(context.getMltXmlContent().contains("<entry producer=\"producer0\"/>"));
    }

    @Then("the MLT XML should contain entry in={string} out={string}")
    public void mltXmlShouldContainTrimEntry(String in, String out) {
        assertTrue(context.getMltXmlContent().contains("in=\"" + in + "\" out=\"" + out + "\""));
    }

    @Then("the MLT XML should contain a producer resource")
    public void mltXmlShouldContainProducer() {
        assertTrue(context.getMltXmlContent().contains("<producer id=\"producer"));
        assertTrue(context.getMltXmlContent().contains("<property name=\"resource\">"));
    }

    @Then("the MLT XML should contain a playlist entry")
    public void mltXmlShouldContainPlaylistEntry() {
        assertTrue(context.getMltXmlContent().contains("<entry producer=\"producer"));
    }
}
