package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;

/**
 * A short two-line banner at the top centre: a round starting, a round won or lost. It drops in, holds and
 * leaves; {@link #pulse()} gives it the small scale pulse that confirms a success.
 */
public final class TopBanner implements Overlay {

    public static final int TOP = 24;
    public static final int HEIGHT = 34;
    public static final int MIN_WIDTH = 132;
    private static final int PADDING = 10;
    private static final int DROP = 6;
    private static final long PULSE_MS = 320;

    private final String heading;
    private final String detail;
    private final int headingColor;
    private final long holdMs;
    private final Transition transition = Motion.transition(Motion.POPOVER_MS);
    private final long shownAt = Clock.SYSTEM.nanos();
    private boolean pulse;

    public TopBanner(String heading, String detail, int headingColor, long holdMs) {
        this.heading = heading;
        this.detail = detail;
        this.headingColor = headingColor;
        this.holdMs = holdMs;
        transition.show();
    }

    public TopBanner pulse() {
        this.pulse = true;
        return this;
    }

    public void dismiss() {
        transition.hide();
    }

    public boolean isShown() {
        return transition.isShowing();
    }

    @Override
    public boolean render(Canvas canvas) {
        long elapsedMs = (Clock.SYSTEM.nanos() - shownAt) / 1_000_000L;
        if (elapsedMs > holdMs) {
            transition.hide();
        }
        float progress = Math.clamp(transition.progress(), 0f, 1f);
        if (!transition.isVisible()) {
            return false;
        }
        Look look = Theme.look();
        Component title = Theme.text(Component.literal(heading), true);
        Component sub = Theme.text(Component.literal(detail), false);
        int width = Math.max(MIN_WIDTH, Math.max(canvas.textWidth(title), canvas.textWidth(sub)) + PADDING * 2);
        int x = (canvas.width() - width) / 2;
        int y = TOP - Math.round((1 - progress) * DROP);

        float scale = 1f;
        if (pulse && elapsedMs < PULSE_MS) {
            // Up to PULSE_SCALE and back, once, as the banner lands.
            float t = elapsedMs / (float) PULSE_MS;
            scale = Lerp.of(1f, Motion.PULSE_SCALE, (float) Math.sin(Math.PI * t));
        }
        canvas.push();
        canvas.scale(scale, x + width / 2f, y + HEIGHT / 2f);
        canvas.pushAlpha(progress);
        look.panel(canvas, x, y, width, HEIGHT);
        canvas.textCentered(title, x + width / 2, y + 7, headingColor, look.textShadow());
        if (!detail.isEmpty()) {
            canvas.textCentered(sub, x + width / 2, y + 19, look.colors().textMuted(), look.textShadow());
        }
        canvas.popAlpha();
        canvas.pop();
        return true;
    }
}
