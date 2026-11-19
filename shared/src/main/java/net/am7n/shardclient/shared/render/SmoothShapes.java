package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.shared.platform.Gfx;

import java.util.HashMap;
import java.util.Map;

final class SmoothShapes {
    private static final int SUPERSAMPLE = 4;

    private static final float SHADOW_STRENGTH = 1f;

    private static final Map<String, Asset> MASKS = new HashMap<>();

    private SmoothShapes() {
    }

    static int guiScale() {
        return Math.max(1, Game.guiScale());
    }

    static void fill(Gfx gfx, int x, int y, int width, int height, float radius, int argb) {
        int g = guiScale();
        int px = x * g;
        int py = y * g;
        int pw = width * g;
        int ph = height * g;
        int r = Math.min(Math.round(radius * g), Math.min(pw, ph) / 2);
        begin(gfx, g);
        if (r <= 0) {
            gfx.fill(px, py, px + pw, py + ph, argb);
        } else {
            Asset disc = mask("disc", r, 0, 2 * r, () -> disc(r));
            corners(gfx, disc, px, py, pw, ph, r, 2 * r, argb);
            gfx.fill(px + r, py, px + pw - r, py + ph, argb);
            gfx.fill(px, py + r, px + r, py + ph - r, argb);
            gfx.fill(px + pw - r, py + r, px + pw, py + ph - r, argb);
        }
        gfx.pop();
    }

    static void frame(Gfx gfx, int x, int y, int width, int height, float radius, int stroke, int argb) {
        int g = guiScale();
        int px = x * g;
        int py = y * g;
        int pw = width * g;
        int ph = height * g;
        int s = Math.max(1, stroke);
        int r = Math.max(s, Math.min(Math.round(radius * g), Math.min(pw, ph) / 2));
        begin(gfx, g);
        Asset ring = mask("ring", r, s, 2 * r, () -> ring(r, s));
        corners(gfx, ring, px, py, pw, ph, r, 2 * r, argb);
        gfx.fill(px + r, py, px + pw - r, py + s, argb);
        gfx.fill(px + r, py + ph - s, px + pw - r, py + ph, argb);
        gfx.fill(px, py + r, px + s, py + ph - r, argb);
        gfx.fill(px + pw - s, py + r, px + pw, py + ph - r, argb);
        gfx.pop();
    }

    static void shadow(Gfx gfx, int x, int y, int width, int height, float radius, float blur, float offsetY,
                       int argb) {
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
        begin(gfx, g);
        Asset mask = mask("shadow", r, b, size, () -> shadowMask(finalR, b));
        int c = r + b;

        blit(gfx, mask, px - b, py - b, 0, 0, c, c, size, argb);
        blit(gfx, mask, px + pw - r, py - b, c + 1, 0, c, c, size, argb);
        blit(gfx, mask, px - b, py + ph - r, 0, c + 1, c, c, size, argb);
        blit(gfx, mask, px + pw - r, py + ph - r, c + 1, c + 1, c, c, size, argb);

        stretch(gfx, mask, px + r, py - b, pw - 2 * r, c, c, 0, 1, c, size, argb);
        stretch(gfx, mask, px + r, py + ph - r, pw - 2 * r, c, c, c + 1, 1, c, size, argb);
        stretch(gfx, mask, px - b, py + r, c, ph - 2 * r, 0, c, c, 1, size, argb);
        stretch(gfx, mask, px + pw - r, py + r, c, ph - 2 * r, c + 1, c, c, 1, size, argb);

        if (pw > 2 * r && ph > 2 * r) {
            int centre = Math.round(shadowAlpha(-r, b) * 255) << 24 | 0xFFFFFF;
            gfx.fill(px + r, py + r, px + pw - r, py + ph - r, multiply(argb, centre));
        }
        gfx.pop();
    }

    static void image(Gfx gfx, Asset texture, int textureSize, int x, int y, int size, int argb) {
        int g = guiScale();
        begin(gfx, g);
        gfx.blit(texture, x * g, y * g, 0, 0, size * g, size * g, textureSize, textureSize, textureSize, textureSize,
                argb);
        gfx.pop();
    }

    private static final int GLOW_SIZE = 256;

    static void glow(Gfx gfx, int cx, int cy, int rx, int ry, int argb) {
        int g = guiScale();
        Asset mask = mask("glow", GLOW_SIZE, 0, GLOW_SIZE, SmoothShapes::glowMask);
        begin(gfx, g);
        gfx.blit(mask, (cx - rx) * g, (cy - ry) * g, 0, 0, 2 * rx * g, 2 * ry * g, GLOW_SIZE, GLOW_SIZE, GLOW_SIZE,
                GLOW_SIZE, argb);
        gfx.pop();
    }

    private static int[] glowMask() {
        int[] image = new int[GLOW_SIZE * GLOW_SIZE];
        float c = GLOW_SIZE / 2f;
        for (int py = 0; py < GLOW_SIZE; py++) {
            for (int px = 0; px < GLOW_SIZE; px++) {
                float dx = (px + 0.5f - c) / c;
                float dy = (py + 0.5f - c) / c;
                float d2 = dx * dx + dy * dy;

                float alpha = d2 >= 1 ? 0 : (float) (Math.exp(-3.5 * d2) - Math.exp(-3.5)) / (float) (1 - Math.exp(-3.5));
                image[py * GLOW_SIZE + px] = white(alpha);
            }
        }
        return image;
    }

    private static void begin(Gfx gfx, int g) {
        gfx.push();
        gfx.scale(1f / g, 1f / g);
    }

    private static void corners(Gfx gfx, Asset mask, int px, int py, int pw, int ph, int r, int size, int argb) {
        blit(gfx, mask, px, py, 0, 0, r, r, size, argb);
        blit(gfx, mask, px + pw - r, py, r, 0, r, r, size, argb);
        blit(gfx, mask, px, py + ph - r, 0, r, r, r, size, argb);
        blit(gfx, mask, px + pw - r, py + ph - r, r, r, r, r, size, argb);
    }

    private static void blit(Gfx gfx, Asset mask, int x, int y, int u, int v, int w, int h, int size, int argb) {
        gfx.blit(mask, x, y, u, v, w, h, w, h, size, size, argb);
    }

    private static void stretch(Gfx gfx, Asset mask, int x, int y, int w, int h, int u, int v, int uw, int vh,
                                int size, int argb) {
        if (w > 0 && h > 0) {
            gfx.blit(mask, x, y, u, v, w, h, uw, vh, size, size, argb);
        }
    }

    private interface Painter {
        int[] paint();
    }

    private static Asset mask(String kind, int a, int b, int size, Painter painter) {
        String key = kind + "_" + a + "_" + b;
        Asset id = MASKS.get(key);
        if (id == null) {
            id = Asset.shared("dynamic/" + key);
            Gfx.registerTexture(id, size, size, painter.paint());
            MASKS.put(key, id);
        }
        return id;
    }

    private static int[] disc(int r) {
        int size = 2 * r;
        int[] image = new int[size * size];
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                image[py * size + px] = white(coverage(px, py, r, r, r, 0));
            }
        }
        return image;
    }

    private static int[] ring(int r, int s) {
        int size = 2 * r;
        int[] image = new int[size * size];
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                image[py * size + px] = white(coverage(px, py, r, r, r, r - s));
            }
        }
        return image;
    }

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

    private static int[] shadowMask(int r, int b) {
        int size = 2 * (r + b) + 1;
        float c = r + b + 0.5f;
        int[] image = new int[size * size];
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float dx = px + 0.5f - c;
                float dy = py + 0.5f - c;
                float distance = (float) Math.sqrt(dx * dx + dy * dy) - r;
                image[py * size + px] = white(shadowAlpha(distance, b) * SHADOW_STRENGTH);
            }
        }
        return image;
    }

    private static float shadowAlpha(float distance, float blur) {
        float t = Maths.clamp((distance + blur / 2f) / blur, 0f, 1f);
        return 1 - t * t * (3 - 2 * t);
    }

    private static int multiply(int argb, int mask) {
        int alpha = Math.round((argb >>> 24) * ((mask >>> 24) / 255f));
        return alpha << 24 | (argb & 0xFFFFFF);
    }

    private static int white(float alpha) {
        return Math.round(Maths.clamp(alpha, 0f, 1f) * 255) << 24 | 0xFFFFFF;
    }
}
