package com.zcpu.tzzmod.client.photo;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import java.util.function.Consumer;

/**
 * Helper to capture a screenshot from the current framebuffer.
 */
public final class ScreenshotHelper {
    private ScreenshotHelper() {}

    public static void takeScreenshot(Framebuffer framebuffer, Consumer<NativeImage> consumer) {
        try {
            ScreenshotRecorder.takeScreenshot(framebuffer, consumer);
        } catch (Exception e) {
            consumer.accept(null);
        }
    }
}
