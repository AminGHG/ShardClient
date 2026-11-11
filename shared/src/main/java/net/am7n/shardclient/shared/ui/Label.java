package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/** One line of text in Minecraft's font. */
public final class Label extends Element {

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    private Component text;
    /** Looked up on every frame, so a theme switch applies at once. */
    private IntSupplier color = () -> Theme.colors().text();
    /** Null: the active look decides. */
    private Boolean shadow;
    private boolean emphasis;
    private Align align = Align.LEFT;

    public Label(Component text) {
        this.text = text;
    }

    public Label(String text) {
        this(Component.literal(text));
    }

    public Label text(Component text) {
        this.text = text;
        return this;
    }

    /** A fixed colour, the same in every theme. */
    public Label color(int argb) {
        this.color = () -> argb;
        return this;
    }

    /** A colour from the theme, e.g. {@code () -> Theme.colors().textMuted()}. */
    public Label color(IntSupplier argb) {
        this.color = argb;
        return this;
    }

    public Label shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /** Heading weight: the semibold font in the website look. */
    public Label emphasis() {
        this.emphasis = true;
        return this;
    }

    public Label align(Align align) {
        this.align = align;
        return this;
    }

    @Override
    public Size measure() {
        var font = Minecraft.getInstance().font;
        return new Size(font.width(Theme.text(text, emphasis)), font.lineHeight - 1);
    }

    @Override
    public void render(Canvas canvas) {
        Component styled = Theme.text(text, emphasis);
        int textWidth = canvas.textWidth(styled);
        int left = switch (align) {
            case LEFT -> x;
            case CENTER -> x + (width - textWidth) / 2;
            case RIGHT -> x + width - textWidth;
        };
        int top = y + (height - (canvas.lineHeight() - 1)) / 2;
        canvas.text(styled, left, top, color.getAsInt(), shadow != null ? shadow : Theme.look().textShadow());
    }
}
