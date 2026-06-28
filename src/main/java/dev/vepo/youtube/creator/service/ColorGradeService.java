package dev.vepo.youtube.creator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.youtube.creator.model.MediaClipEffect;
import dev.vepo.youtube.creator.project.Clip;
import dev.vepo.youtube.creator.project.ClipEffect;
import dev.vepo.youtube.creator.project.ColorGrade;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ColorGradeService {

    public List<ClipEffect> toClipEffects(ColorGrade grade) {
        List<ClipEffect> effects = new ArrayList<>();
        if (grade == null) {
            return effects;
        }
        if (grade.getSaturation() != 1.0 || grade.getHue() != 0) {
            var effect = new ClipEffect("color-grade-hue", "hue");
            Map<String, Object> params = new HashMap<>();
            params.put("level", grade.getHue());
            effect.setParams(params);
            effects.add(effect);
        }
        if (grade.getLift() != 0 || grade.getGamma() != 1.0 || grade.getGain() != 1.0) {
            var effect = new ClipEffect("color-grade-levels", "lift_gamma_gain");
            Map<String, Object> params = new HashMap<>();
            params.put("lift", grade.getLift());
            params.put("gamma", grade.getGamma());
            params.put("gain", grade.getGain());
            effect.setParams(params);
            effects.add(effect);
        }
        if (grade.getLutPath() != null && !grade.getLutPath().isBlank()) {
            var effect = new ClipEffect("color-grade-lut", "lut3d");
            Map<String, Object> params = new HashMap<>();
            params.put("resource", grade.getLutPath());
            effect.setParams(params);
            effects.add(effect);
        }
        return effects;
    }

    public void applyColorGradeToClip(Clip clip) {
        if (clip.getColorGrade() == null) {
            return;
        }
        clip.getEffects().addAll(toClipEffects(clip.getColorGrade()));
    }

    public MediaClipEffect toMediaClipEffect(ClipEffect effect) {
        var mediaEffect = new MediaClipEffect();
        mediaEffect.setId(effect.getId());
        mediaEffect.setMltService(effect.getMltService());
        mediaEffect.setParams(effect.getParams());
        return mediaEffect;
    }
}
