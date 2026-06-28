package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaClipEffect;
import dev.vepo.youtube.creator.model.MediaClipTransition;
import dev.vepo.youtube.creator.model.MediaKeyframe;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.ClipEffect;
import dev.vepo.youtube.creator.project.ClipTransition;
import dev.vepo.youtube.creator.project.Keyframe;
import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.MediaType;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Track;
import dev.vepo.youtube.creator.service.render.MltFrameRate;
import dev.vepo.youtube.creator.service.render.MltFrameRate.Rational;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TimelineAssembler {
    private static final Logger logger = LoggerFactory.getLogger(TimelineAssembler.class);

    @Inject
    MediaService mediaService;

    @Inject
    ColorGradeService colorGradeService;

    public TimelineProject assemble(Project project) throws IOException {
        project.ensureTracks();
        var timeline = new TimelineProject();
        timeline.setId(project.getId().toHexString());
        timeline.setName(project.getName());
        timeline.getTracks().clear();

        for (Track track : project.getTracks()) {
            var mediaTrack = new MediaTrack(track.getIndex(), track.getName(), toTrackType(track.getType()));
            mediaTrack.setLocked(track.isLocked());
            mediaTrack.setVisible(!track.isMuted());
            mediaTrack.setBlendMode(track.getBlendMode());
            timeline.addTrack(mediaTrack);
        }

        var settings = new VideoSettings();
        if (project.getScreenSize() != null) {
            settings.setWidth(project.getScreenSize().getWidth());
            settings.setHeight(project.getScreenSize().getHeight());
        }
        Rational fps = MltFrameRate.fromFrameRate(project.getFrameRate());
        settings.setFrameRateNum(fps.numerator());
        settings.setFrameRateDen(fps.denominator());
        timeline.setVideoSettings(settings);
        timeline.setDuration(project.getDuration() / 1000.0);

        Map<String, java.nio.file.Path> materialized = new HashMap<>();
        if (project.getClips() != null) {
            for (Clip clip : project.getClips()) {
                var media = findMedia(project, clip.getMediaHash());
                if (media == null && clip.getTitle() == null) {
                    logger.warn("Skipping clip {} — media {} not found", clip.getHash(), clip.getMediaHash());
                    continue;
                }
                var track = findTrack(timeline, clip.getTrackIndex(), clip.getType());
                if (track == null) {
                    logger.warn("Skipping clip {} — track {} not found", clip.getHash(), clip.getTrackIndex());
                    continue;
                }

                java.nio.file.Path filePath = null;
                if (media != null) {
                    filePath = materialized.computeIfAbsent(media.getHash(), hash -> {
                        try {
                            return mediaService.materializeMedia(media);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to materialize media " + hash, e);
                        }
                    });
                }

                var mediaClip = new MediaClip();
                mediaClip.setId(clip.getHash());
                if (filePath != null) {
                    mediaClip.setFilePath(filePath.toString());
                }
                mediaClip.setFileName(media != null ? media.getName() : clip.getName());
                mediaClip.setType(toTrackType(clip.getType()));
                mediaClip.setTimelinePosition(clip.getStart() / 1000.0);
                mediaClip.setSpeed(clip.getSpeed() > 0 ? clip.getSpeed() : 1.0);
                mediaClip.setVolume(clip.getVolume() > 0 ? clip.getVolume() / 100.0 : 1.0);
                mapEffects(clip, mediaClip);
                mapTransition(clip, mediaClip);
                mapVolumeKeyframes(clip, mediaClip);

                long sourceIn = clip.getSourceIn() > 0 ? clip.getSourceIn() : 0;
                long sourceDuration = clip.getDuration() > 0 ? clip.getDuration()
                        : (media != null ? media.getDuration() : clip.getDuration());
                long sourceOut = clip.getSourceOut() > 0 ? clip.getSourceOut() : sourceIn + sourceDuration;
                if (sourceOut <= sourceIn && media != null && media.getDuration() > 0) {
                    sourceOut = media.getDuration();
                }

                mediaClip.setStartTime(sourceIn / 1000.0);
                mediaClip.setEndTime(sourceOut / 1000.0);
                mediaClip.setDuration((sourceOut - sourceIn) / 1000.0);
                mediaClip.setTrackIndex(track.getIndex());
                track.addClip(mediaClip);
            }
        }

        timeline.updateDuration();
        return timeline;
    }

    private void mapEffects(Clip clip, MediaClip mediaClip) {
        List<MediaClipEffect> effects = new ArrayList<>();
        if (clip.getEffects() != null) {
            for (ClipEffect effect : clip.getEffects()) {
                effects.add(colorGradeService.toMediaClipEffect(effect));
            }
        }
        if (clip.getColorGrade() != null) {
            for (ClipEffect gradeEffect : colorGradeService.toClipEffects(clip.getColorGrade())) {
                effects.add(colorGradeService.toMediaClipEffect(gradeEffect));
            }
        }
        mediaClip.setEffects(effects);
        if (!effects.isEmpty()) {
            mediaClip.setEffect(effects.getFirst().getMltService());
        }
    }

    private void mapTransition(Clip clip, MediaClip mediaClip) {
        ClipTransition transition = clip.getTransition();
        if (transition == null) {
            return;
        }
        var mediaTransition = new MediaClipTransition();
        mediaTransition.setType(transition.getType());
        mediaTransition.setDurationMs(transition.getDurationMs());
        mediaTransition.setAlignment(transition.getAlignment());
        mediaClip.setClipTransition(mediaTransition);
        mediaClip.setTransition(transition.getType());
    }

    private void mapVolumeKeyframes(Clip clip, MediaClip mediaClip) {
        if (clip.getVolumeKeyframes() == null || clip.getVolumeKeyframes().isEmpty()) {
            return;
        }
        List<MediaKeyframe> keyframes = new ArrayList<>();
        for (Keyframe keyframe : clip.getVolumeKeyframes()) {
            var mediaKeyframe = new MediaKeyframe();
            mediaKeyframe.setTimeMs(keyframe.getTimeMs());
            mediaKeyframe.setValue(keyframe.getValue() / 100.0);
            keyframes.add(mediaKeyframe);
        }
        mediaClip.setVolumeKeyframes(keyframes);
    }

    private Media findMedia(Project project, String mediaHash) {
        if (project.getMedias() == null || mediaHash == null) {
            return null;
        }
        return project.getMedias().stream()
                .filter(m -> mediaHash.equals(m.getHash()))
                .findFirst()
                .orElse(null);
    }

    private MediaTrack findTrack(TimelineProject timeline, int trackIndex, MediaType clipType) {
        var track = timeline.getTrack(trackIndex);
        if (track != null) {
            return track;
        }
        if (MediaType.AUDIO.equals(clipType)) {
            var audioTracks = timeline.getAudioTracks();
            return audioTracks.isEmpty() ? null : audioTracks.getFirst();
        }
        var videoTracks = timeline.getVideoTracks();
        return videoTracks.isEmpty() ? null : videoTracks.getFirst();
    }

    private String toTrackType(MediaType type) {
        if (type == null) {
            return "video";
        }
        return switch (type) {
            case AUDIO -> "audio";
            case VIDEO, IMAGE -> "video";
            default -> "video";
        };
    }
}
