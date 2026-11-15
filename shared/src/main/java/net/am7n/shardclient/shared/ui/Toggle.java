package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Toggle extends Element {
    private static final int WIDTH = 22;
    private static final int HEIGHT = 12;
    private static final int KNOB = 8;

    private final BooleanSupplier state;
    private final Consumer<Boolean> change;
    private final Tween position;
    private BooleanSupplier enabled = () -> true;

    public Toggle(BooleanSupplier state, Consumer<Boolean> change) {
        this.state = state;
        this.change = change;
        this.position = new Tween(state.getAsBoolean() ? 1 : 0, 180, Motion.MOVE);
    }

    public Toggle enabledWhen(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public Size measure() {
        return new Size(WIDTH, HEIGHT);
    }

    @Override
    public void render(Canvas canvas) {
        Palette colors = Theme.colors();
        boolean on = enabled.getAsBoolean();
        if (!on) {
            canvas.pushAlpha(0.35f);
        }
        drawSwitch(canvas, colors, on && state.getAsBoolean());
        if (!on) {
            canvas.popAlpha();
        }
    }

    private void drawSwitch(Canvas canvas, Palette colors, boolean value) {
        position.set(value ? 1 : 0);
        float p = position.get();
        int left = x + (width - WIDTH) / 2;
        int top = y + (height - HEIGHT) / 2;
        int track = Lerp.color(colors.ghostHovered(), colors.primary(), p);
        Theme.look().shape(canvas, left, top, WIDTH, HEIGHT, HEIGHT / 2f, track);
        if (p < 0.5f) {
            Theme.look().outline(canvas, left, top, WIDTH, HEIGHT, HEIGHT / 2f, 1, colors.hairline());
        }

        float stretch = 1 + Motion.KNOB_STRETCH * (float) Math.sin(Math.PI * p);
        int knobWidth = Math.round(KNOB * stretch);
        int travel = WIDTH - 4 - knobWidth;
        int knobX = left + 2 + Math.round(travel * p);
        Theme.look().shape(canvas, knobX, top + 2, knobWidth, KNOB, KNOB / 2f, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY) || !enabled.getAsBoolean()) {
            return false;
        }
        change.accept(!state.getAsBoolean());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.5f));
        return true;
    }
}
