package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * A small rounded button whose text comes from a supplier: cycling through options (left click forward, right
 * click back) or showing a key binding. When its text changes, the old text fades out as the new one fades in.
 */
public final class Pill extends Element {

    private static final int HEIGHT = 14;
    private static final int PADDING = 7;
    private static final int MIN_WIDTH = 44;

    private final Supplier<String> text;
    private final IntConsumer click;
    private final Tween hover = new Tween(0, 110, Easing.OUT_CUBIC);
    private final Tween swap = new Tween(1, 140, Easing.OUT_CUBIC);
    private String shown;
    private String previous = "";
    private boolean active;

    /** @param click receives +1 for a left click and -1 for a right click */
    public Pill(Supplier<String> text, IntConsumer click) {
        this.text = text;
        this.click = click;
        this.shown = text.get();
    }

    /** Highlights the pill, e.g. while it waits for a key. */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Size measure() {
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(Theme.text(Component.literal(text.get()), true));
        return new Size(Math.max(MIN_WIDTH, textWidth + PADDING * 2), HEIGHT);
    }

    @Override
    public void render(Canvas canvas) {
        Look look = Theme.look();
        Palette colors = look.colors();
        String current = text.get();
        if (!current.equals(shown)) {
            previous = shown;
            shown = current;
            swap.snap(0);
            swap.set(1);
        }
        hover.set(contains(canvas.mouseX(), canvas.mouseY()) || active ? 1 : 0);
        int top = y + (height - HEIGHT) / 2;
        int fill = active ? colors.primary() : Lerp.color(colors.ghost(), colors.ghostHovered(), hover.get());
        Theme.look().shape(canvas, x, top, width, HEIGHT, HEIGHT / 2f, fill);
        Theme.look().outline(canvas, x, top, width, HEIGHT, HEIGHT / 2f, 1, colors.hairline());

        int textColor = active ? colors.textOnPrimary() : colors.text();
        float t = swap.get();
        canvas.clip(x, top, width, HEIGHT);
        if (t < 1 && !previous.isEmpty()) {
            canvas.pushAlpha(1 - t);
            canvas.textCentered(Theme.text(Component.literal(previous), true), x + width / 2,
                    top + 3 - Math.round(t * 3), textColor, look.textShadow());
            canvas.popAlpha();
        }
        canvas.pushAlpha(t);
        canvas.textCentered(Theme.text(Component.literal(shown), true), x + width / 2, top + 3 + Math.round((1 - t) * 3),
                textColor, look.textShadow());
        canvas.popAlpha();
        canvas.unclip();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1) || !contains(mouseX, mouseY)) {
            return false;
        }
        click.accept(button == 0 ? 1 : -1);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
        return true;
    }
}
