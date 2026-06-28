package dev.vepo.youtube.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import dev.vepo.youtube.creator.shared.UnitTest;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.model.VideoSettings;

@UnitTest
class VideoEditorControllerExportTest {

    @Test
    void applyExportFormatSetsWebmCodecs() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();

        invokeApplyExportFormat(controller, settings, "webm");

        assertEquals("libvpx-vp9", settings.getVideoCodec());
        assertEquals("libopus", settings.getAudioCodec());
    }

    @Test
    void applyExportFormatSetsMp4Codecs() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();
        settings.setVideoCodec("libvpx-vp9");
        settings.setAudioCodec("libopus");

        invokeApplyExportFormat(controller, settings, "mp4");

        assertEquals("libx264", settings.getVideoCodec());
        assertEquals("aac", settings.getAudioCodec());
    }

    @Test
    void extensionForFormatReturnsExpectedExtensions() throws Exception {
        var controller = new VideoEditorController();
        Method method = VideoEditorController.class.getDeclaredMethod("extensionForFormat", String.class);
        method.setAccessible(true);

        assertEquals("webm", method.invoke(controller, "webm"));
        assertEquals("mov", method.invoke(controller, "mov"));
        assertEquals("mp4", method.invoke(controller, "mp4"));
    }

    @Test
    void applyExportQualityScalesLowTo480p() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);

        invokeApplyExportQuality(controller, settings, "low");

        assertEquals(28, settings.getCrf());
        assertEquals("ultrafast", settings.getPreset());
        assertTrue(settings.getWidth() <= 854);
        assertTrue(settings.getHeight() <= 480);
    }

    private static void invokeApplyExportFormat(VideoEditorController controller, VideoSettings settings,
            String format) throws Exception {
        Method method = VideoEditorController.class.getDeclaredMethod("applyExportFormat", VideoSettings.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, settings, format);
    }

    private static void invokeApplyExportQuality(VideoEditorController controller, VideoSettings settings,
            String quality) throws Exception {
        Method method = VideoEditorController.class.getDeclaredMethod("applyExportQuality", VideoSettings.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, settings, quality);
    }
}
