package com.zcpu.tzzmod.client.photo;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

/**
 * Global camera mode state and HUD overlay renderer.
 * <p>
 * Unlike the previous Screen-based approach, this mode keeps {@code currentScreen}
 * as {@code null} and renders directly in the HUD pipeline, so player movement and
 * mouse look continue to use vanilla game input.
 */
public final class CameraModeClient {
    private static final int CAMERA_FRAME_BLUE = 0xFF2EA8FF;
    private static boolean active;
    private static boolean captureRequested;
    private static boolean pendingDeactivate;

    private CameraModeClient() {
    }

    public static void activate(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (active) {
            return;
        }
        PhotoManager.ensurePhotosDir();
        clearPressedInputs(client);
        active = true;
        captureRequested = false;
        pendingDeactivate = false;
    }

    public static void deactivate(MinecraftClient client) {
        if (!active) {
            return;
        }
        active = false;
        captureRequested = false;
        pendingDeactivate = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(MinecraftClient client) {
        if (!active || client == null) {
            return;
        }
        if (pendingDeactivate) {
            deactivate(client);
            return;
        }
        if (client.player == null || client.world == null || client.currentScreen != null) {
            deactivate(client);
        }
    }

    public static boolean handleKey(MinecraftClient client, int action, KeyInput input) {
        if (!active || client == null) {
            return false;
        }
        if (action == 1 && input.isEscape()) {
            deactivate(client);
            return true;
        }
        if (action == 1 && input.isEnter()) {
            captureRequested = true;
            return true;
        }
        return shouldConsumeKeyInput(client, input);
    }

    public static boolean shouldConsumeMouseClick(Click click) {
        if (!active) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        return client.options.attackKey.matchesMouse(click) || client.options.useKey.matchesMouse(click);
    }

    public static boolean shouldConsumeMouseScroll() {
        return active;
    }

    public static boolean captureCurrentFrame() {
        if (!active || !captureRequested) {
            return false;
        }
        captureRequested = false;
        PhotoManager.capturePhoto(true, getFrameColor());
        pendingDeactivate = true;
        return true;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || pendingDeactivate || client == null || client.player == null || client.currentScreen != null) {
            return;
        }

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        drawVignette(context, sw, sh);
        drawCameraFrame(context, sw, sh);
        drawLabelBox(context, client,
                Text.translatable("phone.tzz_mod.camera.mode"),
                sw / 2, sh / 14, true);
        drawLabelBox(context, client,
                Text.translatable("phone.tzz_mod.camera.hint_exit"),
                sw / 10, sh / 14, false);
        drawLabelBox(context, client,
                Text.translatable("phone.tzz_mod.camera.hint_capture"),
                sw / 2, sh - sh / 10, true);
    }

    private static void drawVignette(DrawContext context, int sw, int sh) {
        int margin = CameraFrameRenderer.getFrameMargin(sw, sh);
        int left = margin;
        int top = margin;
        int right = sw - margin;
        int bottom = sh - margin;
        int vignetteColor = isLightMode() ? 0x40FFFFFF : 0x40000000;

        context.fill(0, 0, sw, top, vignetteColor);
        context.fill(0, bottom, sw, sh, vignetteColor);
        context.fill(0, top, left, bottom, vignetteColor);
        context.fill(right, top, sw, bottom, vignetteColor);
    }

    private static void drawLabelBox(DrawContext context, MinecraftClient client, Text text,
                                     int centerX, int centerY, boolean centered) {
        boolean light = isLightMode();
        int textColor = light ? 0xFF0077AA : 0xFF00FFE0;
        int bgColor = light ? 0xCCE8EDF4 : 0xCC081420;
        int borderColor = light ? 0xDD6699BB : 0xDD1A6A8C;

        String str = text.getString();
        int textW = client.textRenderer.getWidth(str);
        int padX = 10;
        int padY = 5;
        int boxW = textW + padX * 2;
        int boxH = client.textRenderer.fontHeight + padY * 2;

        int boxX = centered ? centerX - boxW / 2 : centerX;
        int boxY = centerY - boxH / 2;

        int cut = Math.max(2, boxH / 4);
        for (int row = 0; row < boxH; row++) {
            int leftCut = 0;
            int rightCut = 0;
            if (row < cut) {
                leftCut = cut - row;
            }
            if (row >= boxH - cut) {
                rightCut = cut - (boxH - 1 - row);
            }
            context.fill(boxX + leftCut, boxY + row, boxX + boxW - rightCut, boxY + row + 1, bgColor);
        }
        context.fill(boxX + cut, boxY, boxX + boxW, boxY + 1, borderColor);
        context.fill(boxX, boxY + boxH - 1, boxX + boxW - cut, boxY + boxH, borderColor);
        for (int i = 0; i < cut; i++) {
            context.fill(boxX + cut - i, boxY + i, boxX + cut - i + 1, boxY + i + 1, borderColor);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(boxX + boxW - cut + i - 1, boxY + boxH - 1 - i,
                    boxX + boxW - cut + i, boxY + boxH - i, borderColor);
        }

        int textX = boxX + padX;
        int textY = boxY + padY;
        if (light) {
            context.drawText(client.textRenderer, text, textX, textY, textColor, false);
        } else {
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, textColor);
        }
    }

    private static void drawCameraFrame(DrawContext context, int sw, int sh) {
        CameraFrameRenderer.drawOnContext(context, sw, sh, getFrameColor());
    }

    public static int getFrameColor() {
        return CAMERA_FRAME_BLUE;
    }

    private static void clearPressedInputs(MinecraftClient client) {
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.pickItemKey.setPressed(false);
        client.options.dropKey.setPressed(false);
        client.options.swapHandsKey.setPressed(false);
        for (var hotbarKey : client.options.hotbarKeys) {
            hotbarKey.setPressed(false);
        }
    }

    private static boolean isLightMode() {
        return PhoneSettingsClient.isLightModeEnabled();
    }

    private static boolean shouldConsumeKeyInput(MinecraftClient client, KeyInput input) {
        if (matchesAny(input,
                client.options.inventoryKey,
                client.options.swapHandsKey,
                client.options.dropKey,
                client.options.pickItemKey,
                client.options.saveToolbarActivatorKey,
                client.options.loadToolbarActivatorKey,
                client.options.advancementsKey,
                client.options.chatKey,
                client.options.commandKey,
                client.options.playerListKey,
                client.options.socialInteractionsKey,
                client.options.attackKey,
                client.options.useKey)) {
            return true;
        }

        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(input)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesAny(KeyInput input, KeyBinding... bindings) {
        for (KeyBinding binding : bindings) {
            if (binding != null && binding.matchesKey(input)) {
                return true;
            }
        }
        return false;
    }
}