package com.zcpu.tzzmod.client.photo;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Full-screen camera mode overlay.
 * <p>
 * When active:
 * - All vanilla HUD is hidden (equivalent to F1)
 * - Player can still move and look around (cursor is locked via lockCursor())
 * - Camera frame lines are drawn on screen
 * - Enter key captures a screenshot
 * - ESC exits camera mode
 * <p>
 * Calling client.mouse.lockCursor() in init() re-locks the cursor even though
 * a Screen is technically open, so mouse look and WASD movement work normally.
 * renderBackground() and blur() are overridden to no-ops to prevent
 * background darkening and blur effects.
 */
public class CameraModeScreen extends Screen {
    private final Screen parentScreen;
    private boolean captureNextFrame = false;
    private boolean captured = false;
    private int frameColor;

    private boolean previousHudHidden;
    private boolean rightMouseWasDown = false;
    /** Ticks since screen opened. Inputs are ignored for the first few ticks
     *  to prevent a stray mouse-click from the phone UI from triggering capture. */
    private int ticksOpen = 0;
    private static final int INPUT_GRACE_TICKS = 5;

    public CameraModeScreen(Screen parent) {
        super(Text.empty());
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        if (client != null) {
            previousHudHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            client.mouse.lockCursor();
        }
        frameColor = isLightMode() ? 0xFF0099CC : 0xFF00FFE0;
        // Initialize rightMouseWasDown to true so that if RMB is already held
        // when the screen opens, we don't immediately trigger capture.
        rightMouseWasDown = true;
        System.out.println("[TZZ Camera] CameraModeScreen.init() — width=" + width + " height=" + height);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        if (client != null) {
            // Keep cursor locked so mouse look works even though a Screen is open.
            if (!client.mouse.isCursorLocked()) {
                client.mouse.lockCursor();
            }
            // Forward movement key states to KeyBinding.
            forwardKeyBinding(client.options.forwardKey);
            forwardKeyBinding(client.options.backKey);
            forwardKeyBinding(client.options.leftKey);
            forwardKeyBinding(client.options.rightKey);
            forwardKeyBinding(client.options.jumpKey);
            forwardKeyBinding(client.options.sneakKey);
        }
    }

    private void forwardKeyBinding(KeyBinding binding) {
        InputUtil.Key key = ((KeyBindingAccessor) binding).tzz_getBoundKey();
        boolean pressed = InputUtil.isKeyPressed(client.getWindow(), key.getCode());
        binding.setPressed(pressed);
    }

    @Override
    public void removed() {
        System.out.println("[TZZ Camera] CameraModeScreen.removed() — ticksOpen=" + ticksOpen);
        if (client != null) {
            client.options.hudHidden = previousHudHidden;
            client.options.forwardKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do not call super.render() — we want a fully transparent overlay
        // (no background darkening, no blur, no widget rendering)

        // Detect right-click via GLFW polling (mouseClicked may not fire when cursor is locked)
        if (client != null && !captured && ticksOpen >= INPUT_GRACE_TICKS) {
            long handle = client.getWindow().getHandle();
            boolean rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (rightDown && !rightMouseWasDown) {
                captureNextFrame = true;
            }
            rightMouseWasDown = rightDown;
        }

        if (captureNextFrame && !captured) {
            captureNextFrame = false;
            captured = true;
            PhotoManager.capturePhoto(true, frameColor);
            if (client != null) {
                client.options.hudHidden = previousHudHidden;
                client.setScreen(null);
            }
            return;
        }

        int sw = this.width;
        int sh = this.height;

        // Draw semi-transparent dark vignette outside the camera frame area
        drawVignette(context, sw, sh);

        // Draw camera frame corner lines (thick and visible)
        drawCameraFrame(context, sw, sh);

        // Draw label boxes
        drawLabelBox(context, Text.translatable("phone.tzz_mod.camera.mode"),
                sw / 2, sh / 14, true);
        drawLabelBox(context, Text.translatable("phone.tzz_mod.camera.hint_exit"),
                sw / 10, sh / 14, false);
        drawLabelBox(context, Text.translatable("phone.tzz_mod.camera.hint_capture"),
                sw / 2, sh - sh / 10, true);
    }

    /** Override to prevent background darkening when a Screen is open. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty — preserve the game world view
    }

    /** Override to prevent blur shader from being applied. */
    @Override
    public void blur() {
        // Intentionally empty — camera overlay must not blur the world
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!captured && ticksOpen >= INPUT_GRACE_TICKS) {
                captureNextFrame = true;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        // Ignore clicks during grace period to prevent stray phone-button clicks
        if (!captured && ticksOpen >= INPUT_GRACE_TICKS) {
            captureNextFrame = true;
            return true;
        }
        return true; // consume but ignore during grace
    }

    @Override
    public void close() {
        if (client != null) {
            client.options.hudHidden = previousHudHidden;
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ── Drawing helpers ──────────────────────────────────────────────

    /**
     * Draws a semi-transparent dark overlay outside the camera frame area,
     * creating a visible vignette that makes the frame boundaries obvious.
     */
    private void drawVignette(DrawContext context, int sw, int sh) {
        int margin = Math.min(sw, sh) / 12;
        int left = margin;
        int top = margin;
        int right = sw - margin;
        int bottom = sh - margin;
        int vignetteColor = isLightMode() ? 0x40FFFFFF : 0x40000000;

        // Top band
        context.fill(0, 0, sw, top, vignetteColor);
        // Bottom band
        context.fill(0, bottom, sw, sh, vignetteColor);
        // Left band (between top and bottom)
        context.fill(0, top, left, bottom, vignetteColor);
        // Right band (between top and bottom)
        context.fill(right, top, sw, bottom, vignetteColor);
    }

    private void drawLabelBox(DrawContext context, Text text, int centerX, int centerY, boolean centered) {
        boolean light = isLightMode();
        int textColor = light ? 0xFF0077AA : 0xFF00FFE0;
        int bgColor = light ? 0xCCE8EDF4 : 0xCC081420;
        int borderColor = light ? 0xDD6699BB : 0xDD1A6A8C;

        String str = text.getString();
        int textW = textRenderer.getWidth(str);
        int padX = 10;
        int padY = 5;
        int boxW = textW + padX * 2;
        int boxH = textRenderer.fontHeight + padY * 2;

        int boxX = centered ? centerX - boxW / 2 : centerX;
        int boxY = centerY - boxH / 2;

        int cut = Math.max(2, boxH / 4);
        // Fill chamfered background
        for (int row = 0; row < boxH; row++) {
            int leftCut = 0, rightCut = 0;
            if (row < cut) leftCut = cut - row;
            if (row >= boxH - cut) rightCut = cut - (boxH - 1 - row);
            context.fill(boxX + leftCut, boxY + row, boxX + boxW - rightCut, boxY + row + 1, bgColor);
        }
        // Top border
        context.fill(boxX + cut, boxY, boxX + boxW, boxY + 1, borderColor);
        // Bottom border
        context.fill(boxX, boxY + boxH - 1, boxX + boxW - cut, boxY + boxH, borderColor);
        // Top-left chamfer
        for (int i = 0; i < cut; i++) {
            context.fill(boxX + cut - i, boxY + i, boxX + cut - i + 1, boxY + i + 1, borderColor);
        }
        // Bottom-right chamfer
        for (int i = 0; i < cut; i++) {
            context.fill(boxX + boxW - cut + i - 1, boxY + boxH - 1 - i,
                    boxX + boxW - cut + i, boxY + boxH - i, borderColor);
        }

        int textX = boxX + padX;
        int textY = boxY + padY;
        if (light) {
            context.drawText(textRenderer, text, textX, textY, textColor, false);
        } else {
            context.drawTextWithShadow(textRenderer, text, textX, textY, textColor);
        }
    }

    private void drawCameraFrame(DrawContext context, int sw, int sh) {
        int margin = Math.min(sw, sh) / 12;
        int lineLen = Math.min(sw, sh) / 5;
        int thickness = Math.max(2, Math.min(sw, sh) / 200);

        int left = margin;
        int top = margin;
        int right = sw - margin;
        int bottom = sh - margin;

        int color = frameColor;
        // Shadow behind lines for contrast
        int shadowColor = isLightMode() ? 0x60000000 : 0x60000000;
        int shadowOff = 1;

        // Shadow pass (offset by 1px)
        // Top-left
        context.fill(left + shadowOff, top + shadowOff, left + lineLen + shadowOff, top + thickness + shadowOff, shadowColor);
        context.fill(left + shadowOff, top + shadowOff, left + thickness + shadowOff, top + lineLen + shadowOff, shadowColor);
        // Top-right
        context.fill(right - lineLen + shadowOff, top + shadowOff, right + shadowOff, top + thickness + shadowOff, shadowColor);
        context.fill(right - thickness + shadowOff, top + shadowOff, right + shadowOff, top + lineLen + shadowOff, shadowColor);
        // Bottom-left
        context.fill(left + shadowOff, bottom - lineLen + shadowOff, left + thickness + shadowOff, bottom + shadowOff, shadowColor);
        context.fill(left + shadowOff, bottom - thickness + shadowOff, left + lineLen + shadowOff, bottom + shadowOff, shadowColor);
        // Bottom-right
        context.fill(right - lineLen + shadowOff, bottom - thickness + shadowOff, right + shadowOff, bottom + shadowOff, shadowColor);
        context.fill(right - thickness + shadowOff, bottom - lineLen + shadowOff, right + shadowOff, bottom + shadowOff, shadowColor);

        // Main color pass
        // Top-left corner
        context.fill(left, top, left + lineLen, top + thickness, color);
        context.fill(left, top, left + thickness, top + lineLen, color);
        // Top-right corner
        context.fill(right - lineLen, top, right, top + thickness, color);
        context.fill(right - thickness, top, right, top + lineLen, color);
        // Bottom-left corner
        context.fill(left, bottom - lineLen, left + thickness, bottom, color);
        context.fill(left, bottom - thickness, left + lineLen, bottom, color);
        // Bottom-right corner
        context.fill(right - lineLen, bottom - thickness, right, bottom, color);
        context.fill(right - thickness, bottom - lineLen, right, bottom, color);
    }

    private boolean isLightMode() {
        return PhoneSettingsClient.isLightModeEnabled();
    }
}
