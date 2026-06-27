package dev.vepo.youtube.creator.support;

import java.nio.file.Path;
import java.util.List;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.project.FrameRate;
import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.MediaType;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.ScreenSize;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.response.Response;

@ScenarioScope
public class ScenarioContext {

    private Project project;
    private Project secondProject;
    private Media media;
    private Media secondMedia;
    private ScreenSize screenSize;
    private FrameRate frameRate;
    private String mimeType;
    private MediaType mediaType;
    private TimelineProject timelineProject;
    private VideoSettings videoSettings;
    private String inputVideoPath;
    private List<TrimOperation> trimOperations;
    private String mltXmlPath;
    private String mltXmlContent;
    private String projectId;
    private Response lastResponse;
    private String resolutionLookup;
    private Path tempMediaFile;
    private double clipEffectiveDuration;
    private double trackTotalDuration;
    private boolean booleanResult;
    private String stringResult;
    private Exception caughtException;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getSecondProject() {
        return secondProject;
    }

    public void setSecondProject(Project secondProject) {
        this.secondProject = secondProject;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public Media getSecondMedia() {
        return secondMedia;
    }

    public void setSecondMedia(Media secondMedia) {
        this.secondMedia = secondMedia;
    }

    public ScreenSize getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSize screenSize) {
        this.screenSize = screenSize;
    }

    public FrameRate getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(FrameRate frameRate) {
        this.frameRate = frameRate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public TimelineProject getTimelineProject() {
        return timelineProject;
    }

    public void setTimelineProject(TimelineProject timelineProject) {
        this.timelineProject = timelineProject;
    }

    public VideoSettings getVideoSettings() {
        return videoSettings;
    }

    public void setVideoSettings(VideoSettings videoSettings) {
        this.videoSettings = videoSettings;
    }

    public String getInputVideoPath() {
        return inputVideoPath;
    }

    public void setInputVideoPath(String inputVideoPath) {
        this.inputVideoPath = inputVideoPath;
    }

    public List<TrimOperation> getTrimOperations() {
        return trimOperations;
    }

    public void setTrimOperations(List<TrimOperation> trimOperations) {
        this.trimOperations = trimOperations;
    }

    public String getMltXmlPath() {
        return mltXmlPath;
    }

    public void setMltXmlPath(String mltXmlPath) {
        this.mltXmlPath = mltXmlPath;
    }

    public String getMltXmlContent() {
        return mltXmlContent;
    }

    public void setMltXmlContent(String mltXmlContent) {
        this.mltXmlContent = mltXmlContent;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public String getResolutionLookup() {
        return resolutionLookup;
    }

    public void setResolutionLookup(String resolutionLookup) {
        this.resolutionLookup = resolutionLookup;
    }

    public Path getTempMediaFile() {
        return tempMediaFile;
    }

    public void setTempMediaFile(Path tempMediaFile) {
        this.tempMediaFile = tempMediaFile;
    }

    public double getClipEffectiveDuration() {
        return clipEffectiveDuration;
    }

    public void setClipEffectiveDuration(double clipEffectiveDuration) {
        this.clipEffectiveDuration = clipEffectiveDuration;
    }

    public double getTrackTotalDuration() {
        return trackTotalDuration;
    }

    public void setTrackTotalDuration(double trackTotalDuration) {
        this.trackTotalDuration = trackTotalDuration;
    }

    public boolean isBooleanResult() {
        return booleanResult;
    }

    public void setBooleanResult(boolean booleanResult) {
        this.booleanResult = booleanResult;
    }

    public String getStringResult() {
        return stringResult;
    }

    public void setStringResult(String stringResult) {
        this.stringResult = stringResult;
    }

    public Exception getCaughtException() {
        return caughtException;
    }

    public void setCaughtException(Exception caughtException) {
        this.caughtException = caughtException;
    }
}
