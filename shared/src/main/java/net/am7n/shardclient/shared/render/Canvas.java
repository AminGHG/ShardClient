package net.am7n.shardclient.shared.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What components draw on for one frame. It is the only class that touches Minecraft's drawing API, and it adds
 * the one thing that API lacks: an opacity that applies to everything drawn while it is set, so a whole panel
 * can fade as one. Positions are GUI pixels; sizes are width and height.
 */
public final class Canvas {

    /** Below this, text and icons are skipped rather than drawn nearly invisible. */
    private static final float MIN_VISIBLE_ALPHA = 0.04f;

    private final GuiGraphicsExtractor graphics;
    private final Font font;
    private final int mouseX;
    private final int mouseY;
    private final float[] alphaStack = new float[16];
    private int alphaDepth;
    private float alpha = 1f;

    public Canvas(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        this.graphics = graphics;
        this.font = font;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
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

    // Opacity

    /** Multiplies the opacity of everything drawn until the matching {@link #popAlpha()}. */
    public void pushAlpha(float factor) {
        alphaStack[alphaDepth++] = alpha;
        alpha *= Math.clamp(factor, 0f, 1f);
    }

    public void popAlpha() {
        alpha = alphaStack[--alphaDepth];
    }

    public float alpha() {
        return alpha;
    }

    private int tint(int argb) {
        return alpha >= 1f ? argb : ARGB.multiplyAlpha(argb, alpha);
    }

    private boolean faint() {
        return alpha < MIN_VISIBLE_ALPHA;
    }

    // Transform

    public void push() {
        graphics.pose().pushMatrix();
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    /** Scales everything drawn until {@link #pop()} around the point {@code (pivotX, pivotY)}. */
    public void scale(float factor, float pivotX, float pivotY) {
        graphics.pose().translate(pivotX, pivotY);
        graphics.pose().scale(factor, factor);
        graphics.pose().translate(-pivotX, -pivotY);
    }

    public void clip(int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    public void unclip() {
        graphics.disableScissor();
    }

    // Shapes and textures

    public void fill(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, tint(argb));
    }

    /** A one pixel frame just inside the rectangle. */
    public void frame(int x, int y, int width, int height, int argb) {
        graphics.outline(x, y, width, height, tint(argb));
    }

    /**
     * A filled rectangle with rounded corners. Built from pixel rows, so at GUI scale the corners are stepped,
     * which is what "round" looks like at Minecraft's resolution.
     */
    public void roundRect(int x, int y, int width, int height, int radius, int argb) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int color = tint(argb);
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
        graphics.fill(x, y + r, x + width, y + height - r, color);
    }

    /** A one pixel outline following the same rounded shape as {@link #roundRect}. */
    public void roundFrame(int x, int y, int width, int height, int radius, int argb) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int color = tint(argb);
        int previous = r;
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            int run = Math.max(1, previous - inset);
            int top = y + row;
            int bottom = y + height - row - 1;
            graphics.fill(x + inset, top, x + inset + run, top + 1, color);
            graphics.fill(x + width - inset - run, top, x + width - inset, top + 1, color);
            graphics.fill(x + inset, bottom, x + inset + run, bottom + 1, color);
            graphics.fill(x + width - inset - run, bottom, x + width - inset, bottom + 1, color);
            previous = inset;
        }
        graphics.fill(x + r, y, x + width - r, y + 1, color);
        graphics.fill(x + r, y + height - 1, x + width - r, y + height, color);
        graphics.fill(x, y + r, x + 1, y + height - r, color);
        graphics.fill(x + width - 1, y + r, x + width, y + height - r, color);
    }

    /** An anti-aliased rounded rectangle drawn at full screen resolution; {@code radius} in GUI pixels. */
    public void smoothRect(int x, int y, int width, int height, float radius, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.fill(graphics, x, y, width, height, radius, tint(argb));
        }
    }

    /** An anti-aliased rounded outline, {@code stroke} screen pixels thick, just inside the rectangle. */
    public void smoothFrame(int x, int y, int width, int height, float radius, int stroke, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.frame(graphics, x, y, width, height, radius, stroke, tint(argb));
        }
    }

    /** A square texture of {@code textureSize} pixels, {@code size} GUI pixels wide. */
    public void image(Identifier texture, int textureSize, int x, int y, int size) {
        if (!faint()) {
            SmoothShapes.image(graphics, texture, textureSize, x, y, size, tint(0xFFFFFFFF));
        }
    }

    /** A soft round glow (radial gradient) centred on {@code (cx, cy)}. */
    public void glow(int cx, int cy, int radiusX, int radiusY, int argb) {
        if (radiusX > 0 && radiusY > 0 && !faint()) {
            SmoothShapes.glow(graphics, cx, cy, radiusX, radiusY, tint(argb));
        }
    }

    /** {@link #glow(int, int, int, int, int)} scaled by {@code amount} (0..1) in size and strength. */
    public void glow(int cx, int cy, int radiusX, int radiusY, int argb, float amount) {
        float a = Math.clamp(amount, 0f, 1f);
        pushAlpha(a);
        glow(cx, cy, Math.round(radiusX * (0.6f + 0.4f * a)), Math.round(radiusY * (0.6f + 0.4f * a)), argb);
        popAlpha();
    }

    /** A vertical gradient from {@code top} to {@code bottom}. */
    public void gradient(int x, int y, int width, int height, int top, int bottom) {
        graphics.fillGradient(x, y, x + width, y + height, tint(top), tint(bottom));
    }

    /** A soft shadow outside a rounded rectangle, like CSS {@code box-shadow}; sizes in GUI pixels. */
    public void smoothShadow(int x, int y, int width, int height, float radius, float blur, float offsetY, int argb) {
        if (width > 0 && height > 0 && !faint()) {
            SmoothShapes.shadow(graphics, x, y, width, height, radius, blur, offsetY, tint(argb));
        }
    }

    /** How far row {@code row} (0 = outermost) of a corner of radius {@code r} starts from the edge. */
    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - dy * dy));
    }

    /** A sprite from Minecraft's GUI atlas, stretched the way its texture says (nine-slice, tile or stretch). */
    public void sprite(Identifier sprite, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint(0xFFFFFFFF));
    }

    public void sprite(Identifier sprite, int x, int y, int width, int height, int argb) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint(argb));
    }

    /** A 16x16 item icon. Items cannot be drawn translucent, so a mostly faded canvas skips them. */
    public void item(ItemStack stack, int x, int y) {
        if (alpha >= 0.5f && !stack.isEmpty()) {
            graphics.fakeItem(stack, x, y);
        }
    }

    /** A player's face (with the hat layer), {@code size} pixels square. */
    public void face(PlayerSkin skin, int x, int y, int size) {
        if (!faint()) {
            PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, size, tint(0xFFFFFFFF));
        }
    }

    // Text

    public int textWidth(Component text) {
        return font.width(text);
    }

    public int lineHeight() {
        return font.lineHeight;
    }

    public void text(Component text, int x, int y, int argb, boolean shadow) {
        if (!faint()) {
            graphics.text(font, text, x, y, tint(argb), shadow);
        }
    }

    /** {@code text} broken into lines no wider than {@code width}. */
    public List<FormattedCharSequence> wrap(Component text, int width) {
        return font.split(text, Math.max(1, width));
    }

    /** One line produced by {@link #wrap}. */
    public void line(FormattedCharSequence line, int x, int y, int argb, boolean shadow) {
        if (!faint()) {
            graphics.text(font, line, x, y, tint(argb), shadow);
        }
    }

    public void textRight(Component text, int right, int y, int argb, boolean shadow) {
        text(text, right - font.width(text), y, argb, shadow);
    }

    public void textCentered(Component text, int centerX, int y, int argb, boolean shadow) {
        text(text, centerX - font.width(text) / 2, y, argb, shadow);
    }
}
