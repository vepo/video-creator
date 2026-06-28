package dev.vepo.youtube.creator.shared;

import java.time.Instant;

import org.bson.types.ObjectId;

import org.bson.types.ObjectId;

import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.ClipEffect;
import dev.vepo.youtube.creator.project.ClipTransition;
import dev.vepo.youtube.creator.project.ColorGrade;
import dev.vepo.youtube.creator.project.Keyframe;
import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.MediaType;
import dev.vepo.youtube.creator.project.Project;

public final class ProjectFixtures {

    private ProjectFixtures() {
    }

    public static Project withVideoClip() {
        var project = new Project();
        project.setId(new ObjectId());
        project.setName("Fixture project");
        project.ensureTracks();

        var media = new Media(new ObjectId(), "scene.mp4", "media-hash-001", "video/mp4", 10_000, Instant.now());
        media.setType(MediaType.VIDEO);
        project.getMedias().add(media);

        var clip = baseClip("clip-001", media.getHash(), "Opening scene");
        clip.setDuration(5_000);
        clip.setSourceIn(500);
        clip.setSourceOut(5_500);
        clip.setStart(1_000);
        clip.setSpeed(1.25);
        clip.setVolume(80);
        clip.setTrackIndex(0);
        project.getClips().add(clip);
        return project;
    }

    public static Project withEnrichedClip() {
        var project = withVideoClip();
        var clip = project.getClips().getFirst();

        clip.setTransition(new ClipTransition("dissolve", 400, "center"));
        clip.getVolumeKeyframes().add(new Keyframe(500, 50));
        clip.getEffects().add(new ClipEffect("blur", "boxblur"));

        var grade = new ColorGrade();
        grade.setHue(12);
        grade.setLift(0.05);
        grade.setGamma(1.1);
        grade.setGain(1.2);
        grade.setLutPath("/luts/cinematic.cube");
        clip.setColorGrade(grade);
        return project;
    }

    public static Clip clipMissingMedia() {
        var clip = baseClip("orphan-clip", "missing-media", "Orphan");
        clip.setDuration(2_000);
        clip.setTrackIndex(0);
        return clip;
    }

    private static Clip baseClip(String hash, String mediaHash, String name) {
        var clip = new Clip();
        clip.setHash(hash);
        clip.setMediaHash(mediaHash);
        clip.setName(name);
        clip.setType(MediaType.VIDEO);
        return clip;
    }
}
