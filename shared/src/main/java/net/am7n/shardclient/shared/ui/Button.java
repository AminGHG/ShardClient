package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * A button drawn by the active look. {@link Style#PRIMARY} is the one main action, {@link Style#GHOST} everything
 * else. It may carry an icon (then icon and label sit left-aligned) or be icon-only (square, with its label as a
 * tooltip). Hover and focus fade in and nudge the icon toward the label; pressing scales it to 0.97 at once
 * (nothing tweens while held) and it eases back on release.
 */
public final class Button extends Element implements Focusable {

    public enum Style {
        PRIMARY,
        GHOST
    }

    private static final int PADDING = 10;
    private static final int ICON = 10;
    private static final int ICON_GAP = 7;

    private Component label;
    private final Runnable onPress;
    private final Tween highlight = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private final Tween pressScale = new Tween(1, Motion.PRESS_RELEASE_MS, Motion.ENTER);
    private Style style = Style.GHOST;
    private Identifier icon;
    private boolean iconOnly;
    private int height = Theme.BUTTON_HEIGHT;
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

    /** An icon from {@link net.am7n.shardclient.shared.render.Icons} before the label. */
    public Button icon(Identifier icon) {
        this.icon = icon;
        return this;
    }

    /** Shows only the icon; the label becomes the tooltip. */
    public Button iconOnly(Identifier icon) {
        this.icon = icon;
        this.iconOnly = true;
        return this;
    }

    /** A taller button for a screen's main actions. */
    public Button tall() {
        this.height = 26;
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
        if (iconOnly) {
            return new Size(height, height);
        }
        int textWidth = Minecraft.getInstance().font.width(labelText());
        int iconSpace = icon != null ? ICON + ICON_GAP : 0;
        return new Size(textWidth + iconSpace + PADDING * 2, height);
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
        int muted = enabled && !primary ? Lerp.color(look.colors().textMuted(), color, amount) : color;
        Component text = labelText();
        boolean shadow = look.textShadow() || (Theme.minecraftButtonFont() && !(primary && Theme.primaryStyle() == Theme.PrimaryStyle.WHITE));
        int textY = y + (height - 8) / 2;

        if (iconOnly) {
            canvas.icon(icon, x + (width - ICON - 2) / 2, y + (height - ICON - 2) / 2, ICON + 2, muted);
            if (hovered) {
                int tipWidth = canvas.textWidth(text) + 12;
                int tipX = Math.max(2, Math.min(x + (width - tipWidth) / 2, canvas.width() - tipWidth - 2));
                int tipY = y - 18 >= 2 ? y - 18 : y + height + 4;
                canvas.pushAlpha(amount);
                canvas.smoothRect(tipX, tipY, tipWidth, 14, 5f, look.colors().surface());
                canvas.text(text, tipX + 6, tipY + 3, look.colors().text(), look.textShadow());
                canvas.popAlpha();
            }
            return;
        }
        if (icon != null) {
            int nudge = Math.round(amount * 2);
            canvas.icon(icon, x + PADDING + nudge, y + (height - ICON) / 2, ICON, muted);
            canvas.text(text, x + PADDING + ICON + ICON_GAP, textY, color, shadow);
        } else {
            canvas.textCentered(text, x + width / 2, textY, color, shadow);
        }
    }

    /** The label in the look's font, or Minecraft's when that is chosen for buttons. */
    private Component labelText() {
        return Theme.minecraftButtonFont() ? label : Theme.text(label, true);
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
