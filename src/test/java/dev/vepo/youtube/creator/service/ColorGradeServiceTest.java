package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.ClipEffect;
import dev.vepo.youtube.creator.project.ColorGrade;
import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class ColorGradeServiceTest {

    private final ColorGradeService service = new ColorGradeService();

    @Test
    void nullGradeProducesNoEffects() {
        assertTrue(service.toClipEffects(null).isEmpty());
    }

    @Test
    void defaultGradeProducesNoEffects() {
        assertTrue(service.toClipEffects(new ColorGrade()).isEmpty());
    }

    @Test
    void hueShiftCreatesHueEffect() {
        var grade = new ColorGrade();
        grade.setHue(15);

        var effects = service.toClipEffects(grade);

        assertEquals(1, effects.size());
        assertEquals("hue", effects.getFirst().getMltService());
        assertEquals(15.0, effects.getFirst().getParams().get("level"));
    }

    @Test
    void liftGammaGainCreatesLevelsEffect() {
        var grade = new ColorGrade();
        grade.setLift(0.1);
        grade.setGamma(1.2);
        grade.setGain(1.3);

        var effects = service.toClipEffects(grade);

        assertEquals(1, effects.size());
        assertEquals("lift_gamma_gain", effects.getFirst().getMltService());
    }

    @Test
    void lutPathCreatesLutEffect() {
        var grade = new ColorGrade();
        grade.setLutPath("/luts/film.cube");

        var effects = service.toClipEffects(grade);

        assertEquals(1, effects.size());
        assertEquals("lut3d", effects.getFirst().getMltService());
        assertEquals("/luts/film.cube", effects.getFirst().getParams().get("resource"));
    }

    @Test
    void fullGradeCreatesMultipleEffects() {
        var grade = new ColorGrade();
        grade.setHue(5);
        grade.setLift(0.05);
        grade.setLutPath("/luts/test.cube");

        assertEquals(3, service.toClipEffects(grade).size());
    }

    @Test
    void applyColorGradeAddsEffectsToClip() {
        var clip = new Clip();
        var grade = new ColorGrade();
        grade.setHue(10);
        clip.setColorGrade(grade);

        service.applyColorGradeToClip(clip);

        assertEquals(1, clip.getEffects().size());
    }

    @Test
    void toMediaClipEffectCopiesFields() {
        var source = new ClipEffect("id-1", "brightness");
        source.getParams().put("level", 1.5);

        var mapped = service.toMediaClipEffect(source);

        assertEquals("id-1", mapped.getId());
        assertEquals("brightness", mapped.getMltService());
        assertEquals(1.5, mapped.getParams().get("level"));
    }
}
