package dev.vepo.youtube.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.model.VideoSettings;

class VideoEditorControllerPreviewTest {

    @Test
    void scalesPreviewResolutionDownTo640x360() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);

        invokeScalePreviewResolution(controller, settings, 640, 360);

        assertEquals(640, settings.getWidth());
        assertEquals(360, settings.getHeight());
    }

    @Test
    void keepsSmallPreviewResolutionWhenAlreadyBelowCap() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();
        settings.setWidth(480);
        settings.setHeight(270);

        invokeScalePreviewResolution(controller, settings, 640, 360);

        assertEquals(480, settings.getWidth());
        assertEquals(270, settings.getHeight());
    }

    @Test
    void applyPreviewQualityUsesLowQualityPreset() throws Exception {
        var controller = new VideoEditorController();
        var settings = new VideoSettings();
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setCrf(18);
        settings.setPreset("slow");

        Method applyPreviewQuality = VideoEditorController.class.getDeclaredMethod("applyPreviewQuality", VideoSettings.class);
        applyPreviewQuality.setAccessible(true);
        applyPreviewQuality.invoke(controller, settings);

        assertEquals(28, settings.getCrf());
        assertEquals("ultrafast", settings.getPreset());
        assertTrue(settings.getWidth() <= 640);
        assertTrue(settings.getHeight() <= 360);
    }

    private static void invokeScalePreviewResolution(VideoEditorController controller, VideoSettings settings,
            int maxWidth, int maxHeight) throws Exception {
        Method scale = VideoEditorController.class.getDeclaredMethod(
                "scalePreviewResolution", VideoSettings.class, int.class, int.class);
        scale.setAccessible(true);
        scale.invoke(controller, settings, maxWidth, maxHeight);
    }

}
