package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A slider with its value as text beside it. While held, the knob follows the pointer exactly (nothing tweens
 * under the finger) and stretches slightly past the ends like a rubber band; released, it settles.
 */
public final class Slider extends Element {

    private static final int TRACK_WIDTH = 84;
    private static final int HEIGHT = 12;
    private static final int KNOB = 8;
    private static final int LABEL_WIDTH = 40;
    /** How far (in pixels) the knob may be pulled past an end. */
    private static final float OVERSCROLL = 4f;

    private final double min;
    private final double max;
    private final DoubleSupplier value;
    private final DoubleConsumer change;
    private final Supplier<String> label;
    private final Tween settle = new Tween(0, 160, Motion.ENTER);
    private boolean dragging;
    private float overshoot;

    public Slider(double min, double max, DoubleSupplier value, DoubleConsumer change, Supplier<String> label) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.change = change;
        this.label = label;
    }

    @Override
    public Size measure() {
        return new Size(LABEL_WIDTH + 6 + TRACK_WIDTH, HEIGHT);
    }

    private int trackLeft() {
        return x + width - TRACK_WIDTH;
    }

    private float fraction() {
        return max > min ? (float) Math.clamp((value.getAsDouble() - min) / (max - min), 0, 1) : 0;
    }

    @Override
    public void render(Canvas canvas) {
        Look look = Theme.look();
        Palette colors = look.colors();
        int left = trackLeft();
        int mid = y + height / 2;
        float offset = dragging ? overshoot : settle.get();
        int knobX = left + Math.round((TRACK_WIDTH - KNOB) * fraction() + offset);

        canvas.textRight(Theme.text(Component.literal(label.get()), false), left - 6, mid - 4, colors.textMuted(),
                look.textShadow());
        Theme.look().shape(canvas, left, mid - 2, TRACK_WIDTH, 4, 2f, colors.ghostHovered());
        Theme.look().shape(canvas, left, mid - 2, Math.max(4, knobX - left + KNOB / 2), 4, 2f, colors.primary());
        float grow = dragging ? 1.15f : 1f;
        int knob = Math.round(KNOB * grow);
        Theme.look().shape(canvas, knobX + (KNOB - knob) / 2, mid - knob / 2, knob, knob, knob / 2f, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || mouseX < trackLeft() - 4 || mouseX > x + width + 4 || mouseY < y || mouseY >= y + height) {
            return false;
        }
        dragging = true;
        follow(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) {
            return false;
        }
        follow(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging) {
            return false;
        }
        dragging = false;
        // Spring back from wherever the rubber band was pulled.
        settle.snap(overshoot);
        settle.set(0);
        overshoot = 0;
        return true;
    }

    private void follow(double mouseX) {
        float travel = TRACK_WIDTH - KNOB;
        float pointer = (float) (mouseX - trackLeft() - KNOB / 2.0);
        float t = pointer / travel;
        change.accept(min + (max - min) * Math.clamp(t, 0f, 1f));
        float past = pointer < 0 ? pointer : pointer > travel ? pointer - travel : 0;
        overshoot = net.am7n.shardclient.shared.anim.Motion.rubberBand(past, OVERSCROLL);
    }
}
