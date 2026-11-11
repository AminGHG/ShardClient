package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;

/**
 * Rating as progress through the rank, animated from before to after the match the way competitive games do it:
 * <ul>
 *   <li>the bar spans the rank's rating range, so it moves exactly as far as the rating changed;</li>
 *   <li>a gain fills in a brighter segment on top of the old fill; a loss leaves a red segment that drains
 *       away just after the bar;</li>
 *   <li>the rating number counts in step with the bar, after a short pause;</li>
 *   <li>a rank change runs the bar to the end, flashes and pulses, swaps the rank name with a crossfade, then
 *       continues from the other end in the new rank.</li>
 * </ul>
 * Pure function of time: {@link #render} draws the state at {@code t} ms after the bar appeared.
 */
public final class RankProgressBar {

    public static final int HEIGHT = 36;
    private static final int BAR_HEIGHT = 6;
    private static final long HOLD_MS = 300;
    private static final long FILL_MS = 1100;
    private static final long OVERFLOW_HALF_MS = 700;
    private static final long FLASH_MS = 260;
    private static final long GHOST_DELAY_MS = 250;
    private static final long GHOST_MS = 420;
    private static final int TICKS = 20;

    private final MatchEnded result;
    private final boolean changed;
    private final boolean up;
    private int lastTick = -1;
    private boolean flashPlayed;

    public RankProgressBar(MatchEnded result) {
        this.result = result;
        this.changed = result.rankChanged();
        this.up = result.ratingChange() >= 0;
    }

    /** How long the whole animation takes, from appearing to settled. */
    public long durationMs() {
        long moving = changed ? OVERFLOW_HALF_MS * 2 + FLASH_MS : FILL_MS;
        return HOLD_MS + moving + (up ? 0 : GHOST_DELAY_MS + GHOST_MS);
    }

    /** One moment of the animation: which rank, where the bar is, where the drain ghost is, the rating shown. */
    private record Frame(boolean newRank, float position, float ghost, int rating, float flash) {
    }

    private Frame frame(long t) {
        float startBefore = fraction(result.ratingBefore(), result.beforeFloor(), result.beforeCeiling());
        float endAfter = fraction(result.ratingAfter(), result.afterFloor(), result.afterCeiling());
        long moving = Math.max(0, t - HOLD_MS);

        if (!changed) {
            float p = Math.clamp(moving / (float) FILL_MS, 0f, 1f);
            float position = Lerp.of(startBefore, endAfter, (up ? Motion.ENTER : Motion.MOVE).apply(p));
            return new Frame(false, position, ghost(t, startBefore, endAfter, FILL_MS), ratingAt(false, position), 0);
        }
        float edge = up ? 1f : 0f;
        if (moving < OVERFLOW_HALF_MS) {
            float p = Motion.MOVE.apply(moving / (float) OVERFLOW_HALF_MS);
            float position = Lerp.of(startBefore, edge, p);
            return new Frame(false, position, position, ratingAt(false, position), 0);
        }
        long afterEdge = moving - OVERFLOW_HALF_MS;
        if (afterEdge < FLASH_MS) {
            return new Frame(afterEdge > FLASH_MS / 2, edge, edge, ratingAt(false, edge),
                    1 - afterEdge / (float) FLASH_MS);
        }
        float p = Motion.ENTER.apply(Math.clamp((afterEdge - FLASH_MS) / (float) OVERFLOW_HALF_MS, 0f, 1f));
        float position = Lerp.of(1f - edge, endAfter, p);
        long total = HOLD_MS + OVERFLOW_HALF_MS * 2 + FLASH_MS;
        float ghost = up ? position : ghost(t - (total - FILL_MS), 1f, endAfter, FILL_MS);
        return new Frame(true, position, ghost, ratingAt(true, position), 0);
    }

    /** Where the loss ghost is: it waits at {@code from} until the bar has finished, then drains to it. */
    private static float ghost(long t, float from, float to, long fillMs) {
        if (to >= from) {
            return to;
        }
        long start = HOLD_MS + fillMs + GHOST_DELAY_MS;
        float p = Motion.MOVE.apply(Math.clamp((t - start) / (float) GHOST_MS, 0f, 1f));
        return Lerp.of(from, to, p);
    }

    /** The rating that belongs to bar position {@code position}, so the number and the bar agree exactly. */
    private int ratingAt(boolean newRank, float position) {
        int floor = newRank ? result.afterFloor() : result.beforeFloor();
        int ceiling = newRank ? result.afterCeiling() : result.beforeCeiling();
        int value = Math.round(floor + (ceiling - floor) * position);
        int low = Math.min(result.ratingBefore(), result.ratingAfter());
        int high = Math.max(result.ratingBefore(), result.ratingAfter());
        return Math.clamp(value, low, high);
    }

    private static float fraction(int rating, int floor, int ceiling) {
        if (ceiling <= floor) {
            return 1f;
        }
        return Math.clamp((rating - floor) / (float) (ceiling - floor), 0f, 1f);
    }

    /**
     * Draws the bar {@code width} wide, centred on {@code cx}, at {@code t} ms after it appeared.
     */
    public void render(Canvas canvas, int cx, int y, int width, long t) {
        Frame frame = frame(t);
        Look look = Theme.look();
        Palette colors = look.colors();
        boolean shadow = look.textShadow();
        int x = cx - width / 2;
        sounds(frame, t);

        // Rating and change, counting with the bar.
        int change = result.ratingChange();
        Component rating = Theme.text(Component.literal(frame.rating() + " MMR"), true);
        Component delta = Theme.text(Component.literal((change >= 0 ? "+" : "") + change), true);
        int changeColor = change > 0 ? colors.positive() : change < 0 ? colors.negative() : colors.textMuted();
        int ratingWidth = canvas.textWidth(rating);
        int chipWidth = canvas.textWidth(delta) + 10;
        int headerLeft = cx - (ratingWidth + 6 + chipWidth) / 2;
        canvas.text(rating, headerLeft, y, colors.text(), shadow);
        int chipX = headerLeft + ratingWidth + 6;
        canvas.smoothRect(chipX, y - 3, chipWidth, 14, 7f, colors.ghostHovered());
        canvas.text(delta, chipX + 5, y, changeColor, shadow);

        // The bar, pulsing slightly on a rank change.
        int barY = y + 15;
        float pulse = frame.flash() > 0 ? Lerp.of(1f, Motion.PULSE_SCALE, (float) Math.sin(Math.PI * frame.flash())) : 1f;
        canvas.push();
        canvas.scale(pulse, cx, barY + BAR_HEIGHT / 2f);
        look.barTrack(canvas, x, barY, width, BAR_HEIGHT);
        float base = frame.newRank() ? 0f : fraction(result.ratingBefore(), result.beforeFloor(), result.beforeCeiling());
        if (up) {
            // Old progress solid, the gain on top of it in the lighter accent.
            float solid = Math.min(base, frame.position());
            fillTo(canvas, look, x, barY, width, frame.position(), colors.accent());
            fillTo(canvas, look, x, barY, width, solid, colors.primary());
        } else {
            // What was lost shows in red behind the bar until the ghost has drained to it.
            fillTo(canvas, look, x, barY, width, Math.max(frame.ghost(), frame.position()), colors.negative());
            fillTo(canvas, look, x, barY, width, frame.position(), colors.primary());
        }
        if (frame.flash() > 0) {
            canvas.pushAlpha(frame.flash());
            canvas.smoothRect(x, barY, width, BAR_HEIGHT, BAR_HEIGHT / 2f, colors.text());
            canvas.popAlpha();
        }
        canvas.pop();

        // Rank names: the old one crossfades into the new one when the bar crosses into it.
        int labelY = barY + BAR_HEIGHT + 5;
        float swap = changed ? Math.clamp(frame.newRank() ? 1f : 0f, 0f, 1f) : 1f;
        String rankName = frame.newRank() || !changed ? result.rankAfter() : result.rankBefore();
        canvas.pushAlpha(changed && frame.flash() > 0 ? 1 - Math.abs(frame.flash() * 2 - 1) : 1f);
        canvas.text(Theme.text(Component.literal(rankName), true), x, labelY,
                swap > 0 && changed ? colors.accentStrong() : colors.textMuted(), shadow);
        canvas.popAlpha();
        int ceiling = frame.newRank() || !changed ? result.afterCeiling() : result.beforeCeiling();
        if (ceiling > 0) {
            canvas.textRight(Theme.text(Component.literal("Next at " + ceiling), false), x + width, labelY,
                    colors.textMuted(), shadow);
        }
    }

    private static void fillTo(Canvas canvas, Look look, int x, int y, int width, float fraction, int color) {
        int filled = Math.round(width * Math.clamp(fraction, 0f, 1f));
        if (filled > 0) {
            look.barFill(canvas, x, y, filled, BAR_HEIGHT, color);
        }
    }

    /** Soft ticks rising in pitch while the bar moves, and a level-up sound when it crosses into a new rank. */
    private void sounds(Frame frame, long t) {
        if (t < HOLD_MS || t > durationMs()) {
            return;
        }
        int tick = Math.round(frame.position() * TICKS) + (frame.newRank() ? TICKS : 0);
        if (lastTick >= 0 && tick != lastTick) {
            UiSounds.barTick(frame.position(), up);
        }
        lastTick = tick;
        if (frame.flash() > 0 && !flashPlayed) {
            flashPlayed = true;
            UiSounds.rankChanged(up);
        }
    }
}
