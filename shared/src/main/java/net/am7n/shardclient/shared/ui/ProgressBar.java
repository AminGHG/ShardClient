package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Cycle;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;

/**
 * A bar that fills up, or (when {@link #indeterminate()}) a highlight that travels back and forth for work
 * whose end is not known, such as searching for an opponent.
 */
public final class ProgressBar extends Element {

    /** Width of the travelling highlight, as a share of the bar. */
    private static final float SWEEP_SHARE = 0.3f;

    private final Tween fill = new Tween(0, Theme.NORMAL_MS, Easing.OUT_CUBIC);
    private final Cycle sweep = new Cycle(1600);
    private boolean indeterminate;
    /** Null: the theme's bar colour, looked up when drawing. */
    private Integer color;
    private final int preferredWidth;

    public ProgressBar(int preferredWidth) {
        this.preferredWidth = preferredWidth;
    }

    /** Sets how full the bar is, 0..1; it moves there smoothly. */
    public ProgressBar progress(float progress) {
        indeterminate = false;
        fill.set(Math.clamp(progress, 0f, 1f));
        return this;
    }

    public ProgressBar indeterminate() {
        indeterminate = true;
        return this;
    }

    public ProgressBar color(int argb) {
        this.color = argb;
        return this;
    }

    @Override
    public Size measure() {
        return new Size(preferredWidth, Theme.look().barHeight());
    }

    @Override
    public void render(Canvas canvas) {
        var look = Theme.look();
        int barHeight = look.barHeight();
        int top = y + (height - barHeight) / 2;
        int color = this.color != null ? this.color : Theme.colors().primary();
        look.barTrack(canvas, x, top, width, barHeight);

        if (indeterminate) {
            int span = Math.max(barHeight, Math.round(width * SWEEP_SHARE));
            // Moves from end to end, so it eases in and out.
            int offset = Math.round((width - span) * Easing.IN_OUT_SINE.apply(sweep.wave()));
            look.barFill(canvas, x + offset, top, span, barHeight, color);
        } else {
            look.barFill(canvas, x, top, Math.round(width * fill.get()), barHeight, color);
        }
    }
}
