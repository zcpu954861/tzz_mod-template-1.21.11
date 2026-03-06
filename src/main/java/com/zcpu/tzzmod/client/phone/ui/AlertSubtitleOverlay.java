package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.Deque;

public final class AlertSubtitleOverlay {
    // reveal duration (total time used to type the text) — adjusted previously to ~2100ms
    private static final long REVEAL_DURATION_MS = 2_100L;
    // wait this long AFTER typing completes before starting blinking
    private static final long PRE_BLINK_HOLD_MS = 500L; // 0.5s before blink
    // after the final "show" of blinking, keep visible for this many ms (includes fade window)
    private static final long FINAL_HOLD_MS = 500L; // 0.5s requested
    private static final long FADE_OUT_MS = 140L;

    // blink timing: start with HIDDEN for BLINK_OFF_MS, then SHOW for BLINK_ON_MS, repeat
    private static final long BLINK_OFF_MS = 80L;
    private static final long BLINK_ON_MS = 120L;
    private static final int BLINK_TOGGLES = 4; // hide,show,hide,show

    private static final long DUPLICATE_WINDOW_MS = 1_000L;
    private static final float SCALE = 1.35F;
    private static final int BASE_RGB = 0x63FF8E;

    private static final Deque<Entry> QUEUE = new ArrayDeque<>();

    private static TypingSubtitleAnimator animator;
    private static Entry current;
    private static long lastRenderMs = -1L;
    private static String lastQueuedText = "";
    private static long lastQueuedAtMs;

    // blinking sequence state
    private static boolean blinking = false;
    private static int blinkToggleIndex = 0; // how many toggles executed
    private static boolean visibleDuringBlink = true; // current visibility during blink sequence
    private static long nextBlinkChangeMs = 0L;

    // pre-blink hold (wait after typing finishes before starting blinking)
    private static long preBlinkHoldUntilMs = 0L;
    // hold-until used after blinking finishes to determine fade-out window
    private static long holdUntilMs;

    private AlertSubtitleOverlay() {
    }

    public static void enqueue(Text message) {
        if (!PhoneSettingsClient.isAlertModeEnabled() || message == null) {
            return;
        }

        String text = message.getString().trim();
        if (text.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (text.equals(lastQueuedText) && now - lastQueuedAtMs < DUPLICATE_WINDOW_MS) {
            return;
        }

        lastQueuedText = text;
        lastQueuedAtMs = now;
        QUEUE.addLast(new Entry(text));
        if (animator == null) {
            startNext(now);
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!PhoneSettingsClient.isAlertModeEnabled()) {
            clear();
            return;
        }
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (client.currentScreen instanceof AbstractPhoneScreen) {
            lastRenderMs = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();
        if (animator == null) {
            startNext(now);
            if (animator == null) {
                return;
            }
        }

        if (lastRenderMs > 0L) {
            float deltaSeconds = Math.max(0.0F, (now - lastRenderMs) / 1000.0F);
            animator.tick(deltaSeconds);
        }
        lastRenderMs = now;

        // If animator completed typing, handle pre-blink hold, blinking sequence, and final hold/fade
        if (animator.isFinished()) {
            // if blinking is active, advance it
            if (blinking) {
                while (blinking && now >= nextBlinkChangeMs) {
                    visibleDuringBlink = !visibleDuringBlink;
                    blinkToggleIndex++;

                    if (visibleDuringBlink) playCharSound();

                    if (blinkToggleIndex >= BLINK_TOGGLES) {
                        // blinking finished
                        blinking = false;
                        blinkToggleIndex = 0;
                        nextBlinkChangeMs = 0L;
                        // final hold includes fade
                        holdUntilMs = now + FINAL_HOLD_MS;
                        visibleDuringBlink = true;
                        // reset pre-blink hold just in case
                        preBlinkHoldUntilMs = 0L;
                        break;
                    } else {
                        nextBlinkChangeMs = now + (visibleDuringBlink ? BLINK_ON_MS : BLINK_OFF_MS);
                    }
                }
            } else {
                // not blinking yet: ensure we wait PRE_BLINK_HOLD_MS after typing finishes before blinking
                if (preBlinkHoldUntilMs == 0L) {
                    preBlinkHoldUntilMs = now + PRE_BLINK_HOLD_MS;
                } else if (now >= preBlinkHoldUntilMs) {
                    // start blinking
                    blinking = true;
                    blinkToggleIndex = 0;
                    visibleDuringBlink = false; // start hidden
                    nextBlinkChangeMs = now + BLINK_OFF_MS; // wait off-duration then show
                    // clear pre-blink marker
                    preBlinkHoldUntilMs = 0L;
                }
            }

            // after final hold finishes, move to next entry
            if (holdUntilMs > 0L && now >= holdUntilMs) {
                animator = null;
                current = null;
                holdUntilMs = 0L;
                blinking = false;
                blinkToggleIndex = 0;
                visibleDuringBlink = true;
                nextBlinkChangeMs = 0L;
                preBlinkHoldUntilMs = 0L;
                startNext(now);
                if (animator == null) return;
            }
        }

        // obtain rendered text depending on whether we're in a hidden phase of blinking
        Text rendered = Text.empty();
        if (animator != null) {
            if (animator.isFinished()) {
                if (blinking && !visibleDuringBlink) {
                    rendered = Text.empty();
                } else {
                    rendered = animator.getRenderedText();
                }
            } else {
                rendered = animator.getRenderedText();
            }
        }

        if (rendered.getString().isEmpty()) return;

        int alpha = 0xFF;
        if (holdUntilMs > 0L) {
            long remaining = holdUntilMs - now;
            if (remaining < FADE_OUT_MS) {
                alpha = Math.max(0, Math.min(0xFF, (int) (0xFF * (remaining / (float) FADE_OUT_MS))));
            }
        }

        Text styled = rendered.copy().formatted(Formatting.GREEN);
        int color = (alpha << 24) | BASE_RGB;
        int centerX = context.getScaledWindowWidth() / 2;
        int baseY = context.getScaledWindowHeight() / 2 + 18;
        int textWidth = client.textRenderer.getWidth(styled);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) centerX, (float) baseY);
        context.getMatrices().scale(SCALE, SCALE);
        context.drawTextWithShadow(client.textRenderer, styled, -textWidth / 2, 0, color);
        context.getMatrices().popMatrix();
    }

    public static void clear() {
        QUEUE.clear();
        animator = null;
        current = null;
        lastRenderMs = -1L;
        lastQueuedText = "";
        lastQueuedAtMs = 0L;
        blinking = false;
        blinkToggleIndex = 0;
        visibleDuringBlink = true;
        nextBlinkChangeMs = 0L;
        preBlinkHoldUntilMs = 0L;
        holdUntilMs = 0L;
    }

    private static void startNext(long now) {
        current = QUEUE.pollFirst();
        // reset blink and hold state
        blinking = false;
        blinkToggleIndex = 0;
        visibleDuringBlink = true;
        nextBlinkChangeMs = 0L;
        preBlinkHoldUntilMs = 0L;
        holdUntilMs = 0L;
        lastRenderMs = now;
        if (current == null) {
            animator = null;
            return;
        }

        float charsPerSecond = Math.max(2.0F, current.codePointLength / (REVEAL_DURATION_MS / 1000.0F));
        animator = new TypingSubtitleAnimator(current.text, charsPerSecond, ignored -> playCharSound());
        animator.start();
    }

    private static void playCharSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.18F, 1.9F);
    }

    private record Entry(String text, int codePointLength) {
        private Entry(String text) {
            this(text, text.codePointCount(0, text.length()));
        }
    }
}
