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

public final class Canvas {
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

    public void push() {
        graphics.pose().pushMatrix();
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

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

    public void fill(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, tint(argb));
    }

    public void frame(int x, int y, int width, int height, int argb) {
        graphics.outline(x, y, width, height, tint(argb));
    }

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

    public void smoothRect(int x, int y, int width, int height, float radius, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.fill(graphics, x, y, width, height, radius, tint(argb));
        }
    }

    public void smoothFrame(int x, int y, int width, int height, float radius, int stroke, int argb) {
        if (width > 0 && height > 0) {
            SmoothShapes.frame(graphics, x, y, width, height, radius, stroke, tint(argb));
        }
    }

    public void icon(Identifier icon, int x, int y, int size, int argb) {
        if (!faint()) {
            SmoothShapes.image(graphics, icon, 64, x, y, size, tint(argb));
        }
    }

    public void image(Identifier texture, int textureSize, int x, int y, int size) {
        if (!faint()) {
            SmoothShapes.image(graphics, texture, textureSize, x, y, size, tint(0xFFFFFFFF));
        }
    }

    public void image(Identifier texture, int textureSize, int x, int y, int size, int argb) {
        if (!faint()) {
            SmoothShapes.image(graphics, texture, textureSize, x, y, size, tint(argb));
        }
    }

    public void imageWithHardShadow(Identifier texture, int textureSize, int x, int y, int size, int offset) {
        image(texture, textureSize, x + offset, y + offset, size, 0xFF000000);
        image(texture, textureSize, x, y, size);
    }

    public void glow(int cx, int cy, int radiusX, int radiusY, int argb) {
        if (radiusX > 0 && radiusY > 0 && !faint()) {
            SmoothShapes.glow(graphics, cx, cy, radiusX, radiusY, tint(argb));
        }
    }

    public void glow(int cx, int cy, int radiusX, int radiusY, int argb, float amount) {
        float a = Math.clamp(amount, 0f, 1f);
        pushAlpha(a);
        glow(cx, cy, Math.round(radiusX * (0.6f + 0.4f * a)), Math.round(radiusY * (0.6f + 0.4f * a)), argb);
        popAlpha();
    }

    public void gradient(int x, int y, int width, int height, int top, int bottom) {
        graphics.fillGradient(x, y, x + width, y + height, tint(top), tint(bottom));
    }

    public void smoothShadow(int x, int y, int width, int height, float radius, float blur, float offsetY, int argb) {
        if (width > 0 && height > 0 && !faint()) {
            SmoothShapes.shadow(graphics, x, y, width, height, radius, blur, offsetY, tint(argb));
        }
    }

    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - dy * dy));
    }

    public void sprite(Identifier sprite, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint(0xFFFFFFFF));
    }

    public void texture(com.mojang.blaze3d.pipeline.RenderPipeline pipeline, Identifier texture, int x, int y,
                        int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth,
                        int textureHeight, int argb) {
        if (!faint()) {
            graphics.blit(pipeline, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight,
                    tint(argb));
        }
    }

    public void sprite(Identifier sprite, int x, int y, int width, int height, int argb) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint(argb));
    }

    public void item(ItemStack stack, int x, int y) {
        if (alpha >= 0.5f && !stack.isEmpty()) {
            graphics.fakeItem(stack, x, y);
        }
    }

    public void face(PlayerSkin skin, int x, int y, int size) {
        if (!faint()) {
            PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, size, tint(0xFFFFFFFF));
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
            graphics.text(font, text, x, y, tint(argb), shadow);
        }
    }

    public List<FormattedCharSequence> wrap(Component text, int width) {
        return font.split(text, Math.max(1, width));
    }

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
