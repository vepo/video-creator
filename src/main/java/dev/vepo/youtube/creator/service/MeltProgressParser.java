package dev.vepo.youtube.creator.service;

import java.util.OptionalInt;
import java.util.regex.Pattern;

final class MeltProgressParser {
    private static final Pattern PERCENTAGE = Pattern.compile("percentage:\\s*(\\d+)");

    private MeltProgressParser() {
    }

    static OptionalInt parsePercent(String line) {
        if (line == null || line.isBlank()) {
            return OptionalInt.empty();
        }
        var matcher = PERCENTAGE.matcher(line);
        if (matcher.find()) {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        }
        return OptionalInt.empty();
    }
}
