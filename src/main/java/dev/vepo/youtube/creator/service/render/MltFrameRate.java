package dev.vepo.youtube.creator.service.render;

import dev.vepo.youtube.creator.project.FrameRate;

public final class MltFrameRate {
    private MltFrameRate() {
    }

    public record Rational(int numerator, int denominator) {
    }

    public static Rational fromFrameRate(FrameRate frameRate) {
        if (frameRate == null) {
            return new Rational(30, 1);
        }
        return switch (frameRate) {
            case FPS_23_976 -> new Rational(24000, 1001);
            case FPS_29_97 -> new Rational(30000, 1001);
            case FPS_59_94 -> new Rational(60000, 1001);
            default -> {
                int fps = (int) Math.round(frameRate.getValue());
                yield new Rational(Math.max(1, fps), 1);
            }
        };
    }

    public static int toFrameNumber(double seconds, Rational rational) {
        return (int) Math.round(seconds * rational.numerator() / (double) rational.denominator());
    }
}
