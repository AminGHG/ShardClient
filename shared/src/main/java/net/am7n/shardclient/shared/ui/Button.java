package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * A rounded button in the website's two styles: {@link Style#PRIMARY} (filled purple, for the one
 * main action) and {@link Style#GHOST} (a quiet tinted surface, for everything else). Hover and focus fade in.
 * Clickable with the mouse and, when focused, with Enter or Space.
 */
public final class Button extends Element implements Focusable {

    public enum Style {
        PRIMARY,
        GHOST
    }

    private static final int HORIZONTAL_PADDING = 10;

    private Component label;
    private final Runnable onPress;
    private final Tween highlight = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    /** 1 at rest; jumps to the pressed scale on press (nothing tweens while held) and eases back on release. */
    private final Tween pressScale = new Tween(1, Motion.PRESS_RELEASE_MS, Motion.ENTER);
    private Style style = Style.GHOST;
    private boolean enabled = true;
    private boolean focused;

    public Button(Component label, Runnable onPress) {
        this.label = label;
        this.onPress = onPress;
    }

    public Button(String label, Runnable onPress) {
        this(Component.literal(label), onPress);
    }

    public Button label(Component label) {
        this.label = label;
        return this;
    }

    public Button style(Style style) {
        this.style = style;
        return this;
    }

    public Button enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Size measure() {
        int textWidth = Minecraft.getInstance().font.width(Theme.text(label, true));
        return new Size(textWidth + HORIZONTAL_PADDING * 2, Theme.BUTTON_HEIGHT);
    }

    @Override
    public void render(Canvas canvas) {
        float scale = pressScale.get();
        if (scale == 1f) {
            draw(canvas);
            return;
        }
        canvas.push();
        canvas.scale(scale, x + width / 2f, y + height / 2f);
        draw(canvas);
        canvas.pop();
    }

    private void draw(Canvas canvas) {
        boolean hovered = enabled && contains(canvas.mouseX(), canvas.mouseY());
        highlight.set(hovered || (enabled && focused) ? 1 : 0);
        float amount = highlight.get();
        boolean primary = style == Style.PRIMARY;
        Look look = Theme.look();

        look.button(canvas, x, y, width, height, primary, amount, enabled, enabled && focused);
        int color = look.buttonTextColor(primary, amount, enabled);
        canvas.textCentered(Theme.text(label, true), x + width / 2, y + (height - 8) / 2, color, look.textShadow());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled || !contains(mouseX, mouseY)) {
            return false;
        }
        pressScale.snap(Motion.PRESS_SCALE);
        activate();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        pressScale.set(1);
        return false;
    }

    @Override
    public void activate() {
        if (!enabled) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        onPress.run();
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean canFocus() {
        return enabled && isVisible();
    }

    @Override
    public void collectFocusable(List<Focusable> into) {
        into.add(this);
    }
}
