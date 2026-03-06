package com.zcpu.tzzmod.client.phone.ui;

import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Simple per-character "typing" animator for subtitles.
 * Construct with a target string and charsPerSecond rate. Call tick(deltaSeconds) each frame
 * and use getRenderedText() to obtain the visible portion as a Text.
 * onCharRendered will be invoked once per newly revealed character (provided as a String of that codepoint)
 * and is a good place to play a short sound.
 */
public class TypingSubtitleAnimator {
    private final String fullString;
    private final int[] codePoints;
    private final float secondsPerChar;
    private final Consumer<String> onCharRendered;

    private int revealedCount = 0; // number of codepoints revealed
    private float acc = 0f;
    private boolean running = false;

    public TypingSubtitleAnimator(String fullString, int charsPerSecond, Consumer<String> onCharRendered) {
        this(fullString, (float) charsPerSecond, onCharRendered);
    }

    public TypingSubtitleAnimator(String fullString, float charsPerSecond, Consumer<String> onCharRendered) {
        this.fullString = fullString == null ? "" : fullString;
        this.codePoints = this.fullString.codePoints().toArray();
        this.secondsPerChar = charsPerSecond > 0 ? (1.0f / charsPerSecond) : 0.05f;
        this.onCharRendered = onCharRendered == null ? (s -> {}) : onCharRendered;
    }

    public void start() {
        this.revealedCount = 0;
        this.acc = 0f;
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return running && revealedCount < codePoints.length;
    }

    public void tick(float deltaSeconds) {
        if (!running || revealedCount >= codePoints.length) return;
        acc += deltaSeconds;
        while (acc >= secondsPerChar && revealedCount < codePoints.length) {
            acc -= secondsPerChar;
            // reveal next codepoint
            int cp = codePoints[revealedCount++];
            String s = new String(Character.toChars(cp));
            try {
                onCharRendered.accept(s);
            } catch (Exception ignored) {}
        }
        if (revealedCount >= codePoints.length) {
            running = false;
        }
    }

    public Text getRenderedText() {
        if (revealedCount <= 0) return Text.empty();
        // build string from first revealedCount codepoints
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < revealedCount && i < codePoints.length; i++) {
            sb.appendCodePoint(codePoints[i]);
        }
        return Text.literal(sb.toString());
    }

    public boolean isFinished() {
        return revealedCount >= codePoints.length;
    }
}
