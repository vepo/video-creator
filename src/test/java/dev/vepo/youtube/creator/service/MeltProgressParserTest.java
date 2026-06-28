package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MeltProgressParserTest {

    @Test
    void parsePercentFromMeltProgressLine() {
        var line = "Current Frame:       1234, percentage:         45";
        assertEquals(45, MeltProgressParser.parsePercent(line).orElseThrow());
    }

    @Test
    void parsePercentIgnoresUnrelatedLines() {
        assertTrue(MeltProgressParser.parsePercent("frame= 100 fps=25").isEmpty());
        assertTrue(MeltProgressParser.parsePercent(null).isEmpty());
    }
}
