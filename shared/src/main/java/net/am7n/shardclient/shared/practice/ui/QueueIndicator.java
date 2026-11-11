package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Cycle;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.practice.QueueState;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;

/**
 * A small card at the top of the screen while the player waits in a queue: which queue, how many are in it,
 * how long they have waited (the server's count) and a searching sweep. Drops in from the top edge.
 */
public final class QueueIndicator implements Overlay {

    private static final int WIDTH = 168;
    private static final int HEIGHT = 30;
    private static final int TOP = 6;
    private static final int PADDING = 8;
    /** How far it drops in from above. */
    private static final int DROP = 8;
    private static final float SWEEP_SHARE = 0.3f;

    private final Transition transition = Motion.transition(Motion.POPOVER_MS);
    private final Cycle sweep = new Cycle(1600);
    private QueueState queue;
    /** When the server's wait time last arrived; the shown time counts on from it until the next one. */
    private long updatedAt;
    /** The local count never runs further ahead of the server than this. */
    private static final int MAX_AHEAD_SECONDS = 3;

    public static final int BOTTOM = TOP + HEIGHT;
    public static final int CARD_WIDTH = WIDTH;

    public boolean isShown() {
        return transition.isShowing();
    }

    public void show(QueueState queue) {
        this.queue = queue;
        updatedAt = net.am7n.shardclient.shared.anim.Clock.SYSTEM.nanos();
        transition.show();
    }

    public void update(QueueState queue) {
        this.queue = queue;
        updatedAt = net.am7n.shardclient.shared.anim.Clock.SYSTEM.nanos();
    }

    public void hide() {
        transition.hide();
    }

    public void reset() {
        transition.snap(false);
        queue = null;
    }

    @Override
    public boolean render(Canvas canvas) {
        float progress = Math.clamp(transition.progress(), 0f, 1f);
        if (!transition.isVisible() || queue == null) {
            return false;
        }
        Look look = Theme.look();
        Palette colors = look.colors();
        int x = (canvas.width() - WIDTH) / 2;
        int y = TOP - Math.round((1 - progress) * DROP);

        canvas.pushAlpha(progress);
        look.panel(canvas, x, y, WIDTH, HEIGHT);

        Component name = Theme.text(Component.literal(queue.displayName() + (queue.ranked() ? " Ranked" : "")), true);
        canvas.text(name, x + PADDING, y + 7, colors.text(), look.textShadow());
        if (queue.playersQueued() > 0) {
            Component count = Theme.text(Component.literal(queue.playersQueued() + " in queue"), false);
            canvas.text(count, x + PADDING + canvas.textWidth(name) + 6, y + 7, colors.textMuted(), look.textShadow());
        }
        long ahead = (net.am7n.shardclient.shared.anim.Clock.SYSTEM.nanos() - updatedAt) / 1_000_000_000L;
        int seconds = queue.waitSeconds() + (int) Math.min(ahead, MAX_AHEAD_SECONDS);
        Component time = Theme.text(Component.literal(format(seconds)), false);
        canvas.textRight(time, x + WIDTH - PADDING, y + 7, colors.textMuted(), look.textShadow());

        int barWidth = WIDTH - PADDING * 2;
        int barHeight = Math.min(3, look.barHeight());
        int barY = y + HEIGHT - PADDING - barHeight + 1;
        look.barTrack(canvas, x + PADDING, barY, barWidth, barHeight);
        int span = Math.round(barWidth * SWEEP_SHARE);
        int offset = Math.round((barWidth - span) * Easing.IN_OUT_SINE.apply(sweep.wave()));
        look.barFill(canvas, x + PADDING + offset, barY, span, barHeight, colors.primary());
        canvas.popAlpha();
        return true;
    }

    static String format(int seconds) {
        int safe = Math.max(0, seconds);
        return safe / 60 + ":" + String.format("%02d", safe % 60);
    }
}
