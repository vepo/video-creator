package dev.vepo.youtube.creator.service.render;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaClipEffect;
import dev.vepo.youtube.creator.model.MediaClipTransition;

public final class MltClipFilters {
    private MltClipFilters() {
    }

    public static void writeFilters(Appendable writer, MediaClip clip, int filterIdStart) throws IOException {
        int filterId = filterIdStart;
        if (clip.getVolume() > 0 && Math.abs(clip.getVolume() - 1.0) > 0.01) {
            filterId = writeVolumeFilter(writer, filterId, clip.getVolume());
        }
        if (clip.getEffects() != null && !clip.getEffects().isEmpty()) {
            for (MediaClipEffect effect : clip.getEffects()) {
                filterId = writeEffectFilter(writer, filterId, effect);
            }
        } else if (clip.getEffect() != null && !clip.getEffect().isBlank()) {
            filterId = writeLegacyEffectFilter(writer, filterId, clip.getEffect());
        }
        if (clip.getClipTransition() != null && clip.getClipTransition().getType() != null
                && !clip.getClipTransition().getType().isBlank()) {
            filterId = writeTransitionFilter(writer, filterId, clip.getClipTransition());
        } else if (clip.getTransition() != null && !clip.getTransition().isBlank()) {
            filterId = writeLegacyTransitionFilter(writer, filterId);
        }
    }

    private static int writeVolumeFilter(Appendable writer, int filterId, double volume) throws IOException {
        writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
        writer.append("    <property name=\"mlt_service\">volume</property>\n");
        writer.append(String.format(Locale.US, "    <property name=\"gain\">%.3f</property>\n", volume));
        writer.append("  </filter>\n");
        return filterId;
    }

    private static int writeEffectFilter(Appendable writer, int filterId, MediaClipEffect effect) throws IOException {
        if (effect.getMltService() == null || effect.getMltService().isBlank()) {
            return filterId;
        }
        writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
        writer.append("    <property name=\"mlt_service\">").append(effect.getMltService()).append("</property>\n");
        if (effect.getParams() != null) {
            for (Map.Entry<String, Object> entry : effect.getParams().entrySet()) {
                writer.append("    <property name=\"")
                      .append(entry.getKey())
                      .append("\">")
                      .append(String.valueOf(entry.getValue()))
                      .append("</property>\n");
            }
        }
        writer.append("  </filter>\n");
        return filterId;
    }

    private static int writeLegacyEffectFilter(Appendable writer, int filterId, String effect) throws IOException {
        switch (effect) {
            case "grayscale" -> {
                writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
                writer.append("    <property name=\"mlt_service\">greyscale</property>\n");
                writer.append("  </filter>\n");
            }
            case "blur" -> {
                writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
                writer.append("    <property name=\"mlt_service\">boxblur</property>\n");
                writer.append("    <property name=\"hori\">5</property>\n");
                writer.append("    <property name=\"vert\">5</property>\n");
                writer.append("  </filter>\n");
            }
            case "brightness" -> {
                writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
                writer.append("    <property name=\"mlt_service\">brightness</property>\n");
                writer.append("    <property name=\"level\">0.2</property>\n");
                writer.append("  </filter>\n");
            }
            case "contrast" -> {
                writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
                writer.append("    <property name=\"mlt_service\">contrast</property>\n");
                writer.append("    <property name=\"level\">1.5</property>\n");
                writer.append("  </filter>\n");
            }
            default -> {
                // unknown effect — skip
            }
        }
        return filterId;
    }

    private static int writeTransitionFilter(Appendable writer, int filterId, MediaClipTransition transition)
            throws IOException {
        writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
        String service = mapTransitionService(transition.getType());
        writer.append("    <property name=\"mlt_service\">").append(service).append("</property>\n");
        double durationFrames = Math.max(1, transition.getDurationMs() / 33.0);
        writer.append(String.format(Locale.US, "    <property name=\"duration\">%.0f</property>\n", durationFrames));
        if (transition.getAlignment() != null) {
            writer.append("    <property name=\"alignment\">")
                  .append(transition.getAlignment())
                  .append("</property>\n");
        }
        writer.append("  </filter>\n");
        return filterId;
    }

    private static int writeLegacyTransitionFilter(Appendable writer, int filterId) throws IOException {
        writer.append("  <filter id=\"filter").append(String.valueOf(filterId++)).append("\">\n");
        writer.append("    <property name=\"mlt_service\">fadeOutBrightness</property>\n");
        writer.append("    <property name=\"start\">0.5</property>\n");
        writer.append("    <property name=\"duration\">15</property>\n");
        writer.append("  </filter>\n");
        return filterId;
    }

    private static String mapTransitionService(String type) {
        if (type == null) {
            return "fadeOutBrightness";
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "dissolve" -> "luma";
            case "wipe" -> "wipe";
            case "fade" -> "fadeOutBrightness";
            default -> type;
        };
    }
}
