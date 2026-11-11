package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Stagger;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;

/**
 * The end of a match, shown once the player is back in the lobby: Victory or Defeat, the score, the rating
 * counting from its old to its new value, and the rank (marked when it changed). Pops in, settles in sequence,
 * holds and leaves.
 */
public final class MatchResultCard implements Overlay {

    public static final int TOP = 24;
    public static final int HEIGHT = 78;
    public static final int WIDTH = 220;
    private static final int PADDING = 12;
    private static final long HOLD_MS = 5500;
    private static final long COUNT_MS = 900;
    private static final long PART_MS = 240;
    private static final int SETTLE = 4;

    private final MatchEnded result;
    private final Transition transition = Motion.transition(Motion.POPOVER_MS);
    private final Stagger parts = new Stagger(4, Motion.ROW_STAGGER_MS * 2, PART_MS);
    private final long shownAt = Clock.SYSTEM.nanos();

    public MatchResultCard(MatchEnded result) {
        this.result = result;
        transition.show();
    }

    public boolean isShown() {
        return transition.isShowing();
    }

    public void dismiss() {
        transition.hide();
    }

    @Override
    public boolean render(Canvas canvas) {
        long elapsedMs = (Clock.SYSTEM.nanos() - shownAt) / 1_000_000L;
        if (elapsedMs > HOLD_MS) {
            transition.hide();
        }
        float progress = transition.progress();
        if (!transition.isVisible()) {
            return false;
        }
        Look look = Theme.look();
        Palette colors = look.colors();
        boolean shadow = look.textShadow();
        int x = (canvas.width() - WIDTH) / 2;
        int y = TOP;
        int center = x + WIDTH / 2;
        float linear = Math.clamp(elapsedMs / (float) parts.totalMs(), 0f, 1f);
        boolean rated = result.rated();

        canvas.push();
        canvas.scale(Lerp.of(Motion.POPOVER_START_SCALE, 1f, progress), center, y + HEIGHT / 2f);
        canvas.pushAlpha(Math.clamp(progress, 0f, 1f));
        look.panel(canvas, x, y, WIDTH, HEIGHT);

        part(canvas, linear, 0, () -> canvas.textCentered(Theme.text(Component.literal(heading()), true),
                center, y + PADDING, headingColor(colors), shadow));
        part(canvas, linear, 1, () -> canvas.textCentered(
                Theme.text(Component.literal(result.scoreSelf() + "  –  " + result.scoreOpponent()), true),
                center, y + PADDING + 16, colors.text(), shadow));
        if (rated) {
            part(canvas, linear, 2, () -> {
                // The rating counts up (or down) once the line has settled.
                float count = Easing.OUT_CUBIC.apply(Math.clamp(
                        (elapsedMs - parts.delayMs() * 2) / (float) COUNT_MS, 0f, 1f));
                int rating = Math.round(Lerp.of(result.ratingBefore(), result.ratingAfter(), count));
                int change = result.ratingChange();
                String text = rating + " MMR  (" + (change >= 0 ? "+" : "") + change + ")";
                int color = change > 0 ? colors.positive() : change < 0 ? colors.negative() : colors.textMuted();
                canvas.textCentered(Theme.text(Component.literal(text), false), center, y + PADDING + 36, color,
                        shadow);
            });
        }
        if (!result.rankAfter().isEmpty()) {
            part(canvas, linear, 3, () -> {
                String text = result.rankChanged() ? "New rank: " + result.rankAfter() : result.rankAfter();
                int color = result.rankChanged() ? colors.accentStrong() : colors.textMuted();
                canvas.textCentered(Theme.text(Component.literal(text), result.rankChanged()), center,
                        y + PADDING + 52, color, shadow);
            });
        }
        canvas.popAlpha();
        canvas.pop();
        return true;
    }

    private String heading() {
        return switch (result.result()) {
            case WIN -> "VICTORY";
            case LOSS -> "DEFEAT";
            case CANCELLED -> "MATCH CANCELLED";
            case OTHER -> "MATCH OVER";
        };
    }

    private int headingColor(Palette colors) {
        return switch (result.result()) {
            case WIN -> colors.positive();
            case LOSS -> colors.negative();
            default -> colors.textMuted();
        };
    }

    private void part(Canvas canvas, float linear, int index, Runnable draw) {
        float amount = parts.item(linear, index, Easing.OUT_CUBIC);
        if (amount <= 0) {
            return;
        }
        canvas.push();
        canvas.translate(0, (1 - amount) * SETTLE);
        canvas.pushAlpha(amount);
        draw.run();
        canvas.popAlpha();
        canvas.pop();
    }
}
