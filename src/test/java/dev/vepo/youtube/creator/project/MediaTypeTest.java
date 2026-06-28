package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MediaTypeTest {

    @ParameterizedTest
    @CsvSource({
            "video/mp4, VIDEO",
            "video/webm, VIDEO",
            "audio/mpeg, AUDIO",
            "image/png, IMAGE"
    })
    void resolvesMimeTypeToMediaType(String mime, MediaType expected) {
        assertEquals(expected, MediaType.load(mime));
    }
}
