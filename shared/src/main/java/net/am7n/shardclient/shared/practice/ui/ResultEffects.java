package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;

/**
 * The atmosphere of the result screen, after the ShardPvP website's hero: small squares in the logo's purples
 * drifting through the screen, a burst of shards when a victory lands, and a light sheen across the headline.
 * Everything is a pure function of time and a fixed seed, so there is no state to update and nothing to clean up.
 */
public final class ResultEffects {

    private static final int DRIFT_COUNT = 42;
    private static final int BURST_COUNT = 28;
    private static final long BURST_MS = 1100;
    private static final float GRAVITY = 140f;

    private final boolean victory;

    public ResultEffects(boolean victory) {
        this.victory = victory;
    }

    /** Squares drifting up (victory) or slowly down (defeat), fading in as the screen opens. */
    public void drift(Canvas canvas, Palette colors, long t, float opacity) {
        int width = canvas.width();
        int height = canvas.height();
        int count = victory ? DRIFT_COUNT : DRIFT_COUNT / 2;
        float seconds = t / 1000f;
        for (int i = 0; i < count; i++) {
            float speed = (victory ? 14f : 6f) + rand(i, 1) * (victory ? 26f : 8f);
            float travel = height + 20;
            float along = (rand(i, 2) * travel + seconds * speed) % travel;
            float y = victory ? height + 10 - along : along - 10;
            float x = rand(i, 3) * width + (float) Math.sin(seconds * (0.6f + rand(i, 4)) + i) * 6f;
            int size = 1 + Math.round(rand(i, 5) * 2);
            // Fade near the edges they come from and go to, so none pops in or out.
            float edge = Math.min(along, travel - along) / 40f;
            float alpha = Math.clamp(edge, 0f, 1f) * (0.35f + rand(i, 6) * 0.5f) * opacity;
            int color = victory ? pick(i, colors.accent(), colors.accentStrong(), colors.primary())
                    : pick(i, colors.textMuted(), colors.textDisabled(), colors.negative());
            canvas.pushAlpha(alpha);
            canvas.fill(Math.round(x), Math.round(y), size, size, color);
            canvas.popAlpha();
        }
    }

    /** Shards thrown out of the headline when it lands, falling under gravity and fading. */
    void burst(Canvas canvas, Palette colors, long sinceLanding, int cx, int cy) {
        if (!victory || sinceLanding < 0 || sinceLanding > BURST_MS) {
            return;
        }
        float s = sinceLanding / 1000f;
        float life = sinceLanding / (float) BURST_MS;
        for (int i = 0; i < BURST_COUNT; i++) {
            double angle = -Math.PI * (0.05 + 0.9 * rand(i, 7));
            float speed = 70f + rand(i, 8) * 110f;
            float x = cx + (float) Math.cos(angle) * speed * s * 1.6f;
            float y = cy + (float) Math.sin(angle) * speed * s + GRAVITY * s * s;
            int size = 2 + Math.round(rand(i, 9) * 2);
            canvas.pushAlpha(1 - Easing.IN_CUBIC.apply(life));
            canvas.fill(Math.round(x), Math.round(y), size, size, pick(i, colors.accent(), colors.accentStrong(),
                    colors.text()));
            canvas.popAlpha();
        }
    }

    /**
     * A light band crossing the headline once, drawn as a brighter copy of it clipped to the band (a colour
     * change through a mask).
     *
     * @param progress 0..1 across the headline; outside that range nothing is drawn
     * @param drawHeadline draws the headline in the given colour
     */
    void sheen(Canvas canvas, float progress, int left, int top, int width, int height, int color,
               java.util.function.IntConsumer drawHeadline) {
        if (progress <= 0 || progress >= 1) {
            return;
        }
        int band = Math.max(6, width / 6);
        int x = left - band + Math.round((width + band) * Easing.IN_OUT_SINE.apply(progress));
        canvas.clip(x, top, band, height);
        drawHeadline.accept(color);
        canvas.unclip();
    }

    private static int pick(int i, int a, int b, int c) {
        float r = rand(i, 10);
        return r < 0.4f ? a : r < 0.75f ? b : c;
    }

    /** A fixed pseudo-random number in 0..1 for particle {@code i} and property {@code salt}. */
    private static float rand(int i, int salt) {
        int h = i * 0x9E3779B1 + salt * 0x85EBCA77;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
