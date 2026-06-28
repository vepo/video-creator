package dev.vepo.youtube.creator.service.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vepo.youtube.creator.shared.UnitTest;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.FrameRate;

@UnitTest
class MltFrameRateTest {

    @Test
    void ntscFrameRateUsesRational() {
        var rational = MltFrameRate.fromFrameRate(FrameRate.FPS_29_97);
        assertEquals(30000, rational.numerator());
        assertEquals(1001, rational.denominator());
    }

    @Test
    void integerFrameRateUsesDenominatorOne() {
        var rational = MltFrameRate.fromFrameRate(FrameRate.FPS_30);
        assertEquals(30, rational.numerator());
        assertEquals(1, rational.denominator());
    }
}
