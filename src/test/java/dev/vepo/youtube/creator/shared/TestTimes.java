package dev.vepo.youtube.creator.shared;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed timestamps for deterministic tests. Do not use {@code Instant.now()} or
 * {@code LocalDateTime.now()} in tests — use these constants instead.
 */
public final class TestTimes {

    public static final LocalDateTime REFERENCE = LocalDateTime.of(2026, 5, 15, 12, 0);

    public static final Instant REFERENCE_INSTANT = REFERENCE.toInstant(ZoneOffset.UTC);

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    public static Instant nextInstant() {
        return REFERENCE_INSTANT.plusSeconds(SEQUENCE.getAndIncrement());
    }

    public static void reset() {
        SEQUENCE.set(0);
    }

    private TestTimes() {}
}
