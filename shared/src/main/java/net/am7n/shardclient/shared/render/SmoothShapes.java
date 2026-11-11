package net.am7n.shardclient.shared.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Anti-aliased rounded shapes, drawn at the screen's real resolution instead of Minecraft's GUI grid (where one
 * GUI pixel is 2–4 screen pixels and every curve shows its steps).
 *
 * <p>Each shape is a white mask texture whose alpha is the shape's coverage, generated once per size and tinted
 * when drawn. Corners come from the mask's quadrants; straight parts are plain fills or a stretched mask column.
 * Everything is drawn in screen pixels: the pose is scaled down by the GUI scale for the duration of a call.
 */
final class SmoothShapes {

    /** Samples per axis per pixel when computing coverage. */
    private static final int SUPERSAMPLE = 4;
    /** Peak opacity of the shadow mask; the shadow colour's own alpha scales it further. */
    private static final float SHADOW_STRENGTH = 1f;

    private static final Map<String, Identifier> MASKS = new HashMap<>();

    private SmoothShapes() {
    }

    static int guiScale() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
    }

    /** A filled rounded rectangle; all values in GUI pixels, {@code radius} may be fractional. */
    static void fill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float radius, int argb) {
        int g = guiScale();
        int px = x * g;
        int py = y * g;
        int pw = width * g;
        int ph = height * g;
        int r = Math.min(Math.round(radius * g), Math.min(pw, ph) / 2);
        begin(graphics, g);
        if (r <= 0) {
            graphics.fill(px, py, px + pw, py + ph, argb);
        } else {
            Identifier disc = mask("disc", r, 0, () -> disc(r));
            corners(graphics, disc, px, py, pw, ph, r, 2 * r, argb);
            graphics.fill(px + r, py, px + pw - r, py + ph, argb);
            graphics.fill(px, py + r, px + r, py + ph - r, argb);
            graphics.fill(px + pw - r, py + r, px + pw, py + ph - r, argb);
        }
        end(graphics);
    }

    /**
     * A rounded outline just inside the rectangle, {@code stroke} screen pixels thick (at least 1), so it reads
     * as the website's 1px hairline at any GUI scale.
     */
    static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float radius, int stroke,
                      int argb) {
        int g = guiScale();
        int px = x * g;
        int py = y * g;
        int pw = width * g;
        int ph = height * g;
        int s = Math.max(1, stroke);
        int r = Math.max(s, Math.min(Math.round(radius * g), Math.min(pw, ph) / 2));
        begin(graphics, g);
        Identifier ring = mask("ring", r, s, () -> ring(r, s));
        corners(graphics, ring, px, py, pw, ph, r, 2 * r, argb);
        graphics.fill(px + r, py, px + pw - r, py + s, argb);
        graphics.fill(px + r, py + ph - s, px + pw - r, py + ph, argb);
        graphics.fill(px, py + r, px + s, py + ph - r, argb);
        graphics.fill(px + pw - s, py + r, px + pw, py + ph - r, argb);
        end(graphics);
    }

    /**
     * A soft shadow around (outside) a rounded rectangle, like CSS {@code box-shadow}: {@code blur} GUI pixels
     * wide, moved down by {@code offsetY}.
     */
    static void shadow(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float radius, float blur,
                       float offsetY, int argb) {
        int g = guiScale();
        int r = Math.max(1, Math.round(radius * g));
        int b = Math.max(1, Math.round(blur * g));
        int px = x * g;
        int py = y * g + Math.round(offsetY * g);
        int pw = width * g;
        int ph = height * g;
        r = Math.min(r, Math.min(pw, ph) / 2);
        int size = 2 * (r + b) + 1;
        int finalR = r;
        begin(graphics, g);
        Identifier mask = mask("shadow", r, b, () -> shadowMask(finalR, b));
        int c = r + b;
        // Corners: quadrants of the mask around its centre pixel c (which sits on the corner's arc centre).
        blit(graphics, mask, px - b, py - b, 0, 0, c, c, size, argb);
        blit(graphics, mask, px + pw - r, py - b, c, 0, c, c, size, argb);
        blit(graphics, mask, px - b, py + ph - r, 0, c, c, c, size, argb);
        blit(graphics, mask, px + pw - r, py + ph - r, c, c, c, c, size, argb);
        // Edges: the mask's centre column or row, stretched along the edge.
        stretch(graphics, mask, px + r, py - b, pw - 2 * r, b, c, 0, 1, b, size, argb);
        stretch(graphics, mask, px + r, py + ph, pw - 2 * r, b, c, c + r, 1, b, size, argb);
        stretch(graphics, mask, px - b, py + r, b, ph - 2 * r, 0, c, b, 1, size, argb);
        stretch(graphics, mask, px + pw, py + r, b, ph - 2 * r, c + r, c, b, 1, size, argb);
        end(graphics);
    }

    private static void begin(GuiGraphicsExtractor graphics, int g) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(1f / g, 1f / g);
    }

    private static void end(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    /** The four quadrants of a {@code size}-square mask at the four corners of a {@code r}-cornered rectangle. */
    private static void corners(GuiGraphicsExtractor graphics, Identifier mask, int px, int py, int pw, int ph, int r,
                                int size, int argb) {
        blit(graphics, mask, px, py, 0, 0, r, r, size, argb);
        blit(graphics, mask, px + pw - r, py, r, 0, r, r, size, argb);
        blit(graphics, mask, px, py + ph - r, 0, r, r, r, size, argb);
        blit(graphics, mask, px + pw - r, py + ph - r, r, r, r, r, size, argb);
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier mask, int x, int y, int u, int v, int w, int h,
                             int size, int argb) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, mask, x, y, u, v, w, h, size, size, argb);
    }

    private static void stretch(GuiGraphicsExtractor graphics, Identifier mask, int x, int y, int w, int h, int u,
                                int v, int uw, int vh, int size, int argb) {
        if (w > 0 && h > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, mask, x, y, u, v, w, h, uw, vh, size, size, argb);
        }
    }

    // Masks

    private interface Painter {
        NativeImage paint();
    }

    private static Identifier mask(String kind, int a, int b, Painter painter) {
        String key = kind + "_" + a + "_" + b;
        Identifier id = MASKS.get(key);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath("shardclient-shared", "dynamic/" + key);
            NativeImage image = painter.paint();
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "shardclient " + key, image));
            MASKS.put(key, id);
        }
        return id;
    }

    /** A disc of radius {@code r} filling a 2r square. */
    private static NativeImage disc(int r) {
        int size = 2 * r;
        NativeImage image = new NativeImage(size, size, true);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                image.setPixel(px, py, white(coverage(px, py, r, r, r, 0)));
            }
        }
        return image;
    }

    /** A ring of outer radius {@code r} and width {@code s} filling a 2r square. */
    private static NativeImage ring(int r, int s) {
        int size = 2 * r;
        NativeImage image = new NativeImage(size, size, true);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                image.setPixel(px, py, white(coverage(px, py, r, r, r, r - s)));
            }
        }
        return image;
    }

    /** Fraction of pixel (px, py) inside the circle of radius {@code outer} and outside {@code inner}. */
    private static float coverage(int px, int py, float cx, float cy, float outer, float inner) {
        int inside = 0;
        for (int sy = 0; sy < SUPERSAMPLE; sy++) {
            for (int sx = 0; sx < SUPERSAMPLE; sx++) {
                float dx = px + (sx + 0.5f) / SUPERSAMPLE - cx;
                float dy = py + (sy + 0.5f) / SUPERSAMPLE - cy;
                float d2 = dx * dx + dy * dy;
                if (d2 <= outer * outer && d2 >= inner * inner) {
                    inside++;
                }
            }
        }
        return inside / (float) (SUPERSAMPLE * SUPERSAMPLE);
    }

    /**
     * The shadow of a point-sized rounded square (corner {@code r}) blurred over {@code b}: opacity falls off
     * smoothly with distance from the shape and is zero inside it, so it never darkens a translucent panel.
     */
    private static NativeImage shadowMask(int r, int b) {
        int size = 2 * (r + b) + 1;
        float c = r + b + 0.5f;
        NativeImage image = new NativeImage(size, size, true);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float dx = px + 0.5f - c;
                float dy = py + 0.5f - c;
                float distance = (float) Math.sqrt(dx * dx + dy * dy) - r;
                float t = Math.clamp(distance / b, 0f, 1f);
                float falloff = 1 - t * t * (3 - 2 * t);
                float outside = 1 - coverage(px, py, c, c, r, 0);
                image.setPixel(px, py, white(falloff * outside * SHADOW_STRENGTH));
            }
        }
        return image;
    }

    private static int white(float alpha) {
        return Math.round(Math.clamp(alpha, 0f, 1f) * 255) << 24 | 0xFFFFFF;
    }
}
