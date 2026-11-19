package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.platform.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class Canvas {
    private static final float MIN_VISIBLE_ALPHA = 0.04f;

    private final Gfx gfx;
    private final Font font;
    private final int mouseX;
    private final int mouseY;
    private final float[] alphaStack = new float[16];
    private int alphaDepth;
    private float alpha = 1f;

    public Canvas(Gfx gfx, Font font, int mouseX, int mouseY) {
        this.gfx = gfx;
        this.font = font;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public Gfx gfx() {
        return gfx;
    }

    public int width() {
        return gfx.width();
    }

    public int height() {
        return gfx.height();
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public Font font() {
        return font;
    }

    public void pushAlpha(float factor) {
        alphaStack[alphaDepth++] = alpha;
        alpha *= Maths.clamp(factor, 0f, 1f);
    }

    public void popAlpha() {
        alpha = alphaStack[--alphaDepth];
    }

    public float alpha() {
        return alpha;
    }

    public int tint(int argb) {
        if (alpha >= 1f) {
            return argb;
        }
        int a = Math.round((argb >>> 24) * alpha);
        return a << 24 | (argb & 0xFFFFFF);
    }

    public boolean faint() {
        return alpha < MIN_VISIBLE_ALPHA;
    }

    public void push() {
        gfx.push();
    }

    public void pop() {
        gfx.pop();
    }

    public void translate(float x, float y) {
        gfx.translate(x, y);
    }

    public void scale(float factor, float pivotX, float pivotY) {
        gfx.translate(pivotX, pivotY);
        gfx.scale(factor, factor);
        gfx.translate(-pivotX, -pivotY);
    }

    public void clip(int x, int y, int width, int height) {
        gfx.scissor(x, y, x + width, y + height);
    }

    public void unclip() {
        gfx.unscissor();
    }

    public void fill(int x, int y, int width, int height, int argb) {
        gfx.fill(x, y, x + width, y + height, tint(argb));
    }

    public void frame(int x, int y, int width, int height, int argb) {
        gfx.outline(x, y, width, height, tint(argb));
    }

    public void roundRect(int x, int y, int width, int height, int radius, int argb) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int color = tint(argb);
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            gfx.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            gfx.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
        gfx.fill(x, y + r, x + width, y + height - r, color);
    }

    public void roundFrame(int x, int y, int width, int height, int radius, int argb) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int color = tint(argb);
        int previous = r;
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            int run = Math.max(1, previous - inset);
            int top = y + row;
            int bottom = y + height - row - 1;
            gfx.fill(x + inset, top, x + inset + run, top + 1, color);
            gfx.fill(x + width - inset - run, top, x + width - inset, top + 1, color);
            gfx.fill(x + inset, bottom, x + inset + run, bottom + 1, color);
            gfx.fill(x + width - inset - run, bottom, x + width - inset, bottom + 1, color);
            previous = inset;
        }
        gfx.fill(x + r, y, x + width - r, y + 1, color);
        gfx.fill(x + r, y + height - 1, x + width - r, y + height, color);
        gfx.fill(x, y + r, x + 1, y + height - r, color);
        gfx.fill(x + width - 1, y + r, x + width, y + height - r, color);
    }

    public void smoothRect(int x, int y, int width, int height, float radius, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.fill(gfx, x, y, width, height, radius, tint(argb));
        }
    }

    public void smoothFrame(int x, int y, int width, int height, float radius, int stroke, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.frame(gfx, x, y, width, height, radius, stroke, tint(argb));
        }
    }

    public void icon(Asset icon, int x, int y, int size, int argb) {
        if (!faint()) {
            SmoothShapes.image(gfx, icon, 64, x, y, size, tint(argb));
        }
    }

    public void image(Asset texture, int textureSize, int x, int y, int size) {
        if (!faint()) {
            SmoothShapes.image(gfx, texture, textureSize, x, y, size, tint(0xFFFFFFFF));
        }
    }

    public void image(Asset texture, int textureSize, int x, int y, int size, int argb) {
        if (!faint()) {
            SmoothShapes.image(gfx, texture, textureSize, x, y, size, tint(argb));
        }
    }

    public void imageWithHardShadow(Asset texture, int textureSize, int x, int y, int size, int offset) {
        image(texture, textureSize, x + offset, y + offset, size, 0xFF000000);
        image(texture, textureSize, x, y, size);
    }

    public void glow(int cx, int cy, int radiusX, int radiusY, int argb) {
        if (radiusX > 0 && radiusY > 0 && !faint()) {
            SmoothShapes.glow(gfx, cx, cy, radiusX, radiusY, tint(argb));
        }
    }

    public void glow(int cx, int cy, int radiusX, int radiusY, int argb, float amount) {
        float a = Maths.clamp(amount, 0f, 1f);
        pushAlpha(a);
        glow(cx, cy, Math.round(radiusX * (0.6f + 0.4f * a)), Math.round(radiusY * (0.6f + 0.4f * a)), argb);
        popAlpha();
    }

    public void gradient(int x, int y, int width, int height, int top, int bottom) {
        gfx.gradient(x, y, x + width, y + height, tint(top), tint(bottom));
    }

    public void smoothShadow(int x, int y, int width, int height, float radius, float blur, float offsetY, int argb) {
        if (width > 0 && height > 0 && !faint()) {
            SmoothShapes.shadow(gfx, x, y, width, height, radius, blur, offsetY, tint(argb));
        }
    }

    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - dy * dy));
    }

    public void sprite(Asset sprite, int x, int y, int width, int height) {
        gfx.sprite(sprite, x, y, width, height, tint(0xFFFFFFFF));
    }

    public void sprite(Asset sprite, int x, int y, int width, int height, int argb) {
        gfx.sprite(sprite, x, y, width, height, tint(argb));
    }

    public void item(ItemStack stack, int x, int y) {
        if (alpha >= 0.5f && !stack.isEmpty()) {
            gfx.item(stack, x, y);
        }
    }

    public void face(UUID playerId, int x, int y, int size) {
        if (!faint()) {
            gfx.face(playerId, x, y, size, tint(0xFFFFFFFF));
        }
    }

    public int textWidth(Component text) {
        return font.width(text);
    }

    public int lineHeight() {
        return font.lineHeight;
    }

    public void text(Component text, int x, int y, int argb, boolean shadow) {
        if (!faint()) {
            gfx.text(font, text, x, y, tint(argb), shadow);
        }
    }

    public List<FormattedCharSequence> wrap(Component text, int width) {
        return font.split(text, Math.max(1, width));
    }

    public void line(FormattedCharSequence line, int x, int y, int argb, boolean shadow) {
        if (!faint()) {
            gfx.text(font, line, x, y, tint(argb), shadow);
        }
    }

    public void textRight(Component text, int right, int y, int argb, boolean shadow) {
        text(text, right - font.width(text), y, argb, shadow);
    }

    public void textCentered(Component text, int centerX, int y, int argb, boolean shadow) {
        text(text, centerX - font.width(text) / 2, y, argb, shadow);
    }
}
