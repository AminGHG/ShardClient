package net.am7n.shardclient.full.modules.lyrics;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.full.platform.Glow;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class LyricGlow {
    static final int PAD = 9;
    private static final int RES = 4;
    private static final int HEIGHT = 9;
    static final int QUAD_HEIGHT = HEIGHT + PAD * 2;
    private static final float TIGHT = 0.9f;
    private static final float WIDE = 2.8f;
    private static final int CACHE = 16;

    record Texture(Asset id, int width, int height) {
    }

    private record Key(String text, String font, boolean bold, int width) {
    }

    private record Mask(int width, int height, byte[] alpha) {
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ShardClient lyrics glow");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final Map<Key, CompletableFuture<Mask>> PENDING = new HashMap<>();
    private static final LinkedHashMap<Key, Texture> READY = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Font> FONTS = new HashMap<>();
    private static int counter;

    private LyricGlow() {
    }

    static Texture get(String text, String font, boolean bold, int width) {
        if (text.isBlank() || width <= 0) {
            return null;
        }
        Key key = new Key(text, font, bold, width);
        Texture ready = READY.get(key);
        if (ready != null) {
            return ready;
        }
        CompletableFuture<Mask> job = PENDING.get(key);
        if (job == null) {
            PENDING.put(key, CompletableFuture.supplyAsync(() -> build(key), WORKER));
            return null;
        }
        if (!job.isDone()) {
            return null;
        }
        PENDING.remove(key);
        Mask mask = job.getNow(null);
        if (mask == null) {
            return null;
        }
        Texture texture = upload(mask);
        READY.put(key, texture);
        trim();
        return texture;
    }

    private static void trim() {
        Iterator<Map.Entry<Key, Texture>> it = READY.entrySet().iterator();
        while (READY.size() > CACHE && it.hasNext()) {
            Texture old = it.next().getValue();
            it.remove();
            Glow.release(old.id());
        }
    }

    private static Texture upload(Mask mask) {
        Asset id = Asset.of("shardclient", "lyrics_glow/" + counter++);
        Glow.upload(id, mask.width(), mask.height(), mask.alpha());
        return new Texture(id, mask.width(), mask.height());
    }

    private static Mask build(Key key) {
        try {
            int width = (key.width() + PAD * 2) * RES;
            int height = (HEIGHT + PAD * 2) * RES;
            float[] shape = key.font().equals("Minecraft") ? pixelShape(key, width, height) : fontShape(key, width, height);
            float[] tight = blur(shape, width, height, TIGHT * RES);
            float[] wide = blur(shape, width, height, WIDE * RES);
            float tightMax = max(tight);
            float wideMax = max(wide);
            byte[] alpha = new byte[width * height];
            for (int i = 0; i < alpha.length; i++) {
                float t = tightMax > 0 ? tight[i] / tightMax : 0;
                float w = wideMax > 0 ? wide[i] / wideMax : 0;
                float value = Math.min(1f, 0.7f * t + 0.45f * w);
                alpha[i] = (byte) Math.round(value * 255);
            }
            return new Mask(width, height, alpha);
        } catch (Throwable ex) {
            ShardClientRuntime.LOGGER.warn("Couldn't build the lyrics glow", ex);
            return null;
        }
    }

    private static float[] pixelShape(Key key, int width, int height) {
        float[] shape = new float[width * height];
        int pen = 0;
        String text = key.text();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            VoxelFont.Glyph glyph = VoxelFont.glyph(cp);
            if (glyph == null) {
                pen += 6;
                continue;
            }
            boolean[][] pixels = glyph.pixels();
            for (int row = 0; row < pixels.length; row++) {
                for (int col = 0; col < pixels[row].length; col++) {
                    if (pixels[row][col]) {
                        fill(shape, width, height, pen + col, row);
                        if (key.bold()) {
                            fill(shape, width, height, pen + col + 1, row);
                        }
                    }
                }
            }
            pen += glyph.advance() + (key.bold() && cp != ' ' ? 1 : 0);
        }
        return shape;
    }

    private static void fill(float[] shape, int width, int height, int unitX, int unitY) {
        int x0 = (unitX + PAD) * RES;
        int y0 = (unitY + PAD) * RES;
        for (int y = y0; y < y0 + RES && y < height; y++) {
            for (int x = x0; x < x0 + RES && x < width; x++) {
                shape[y * width + x] = 1;
            }
        }
    }

    private static float[] fontShape(Key key, int width, int height) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Font font = awtFont(key.font()).deriveFont(key.bold() ? Font.BOLD : Font.PLAIN, 11f * RES);
        FontRenderContext context = g.getFontRenderContext();
        double advance = font.getStringBounds(key.text(), context).getWidth();
        double scale = advance <= 0 ? 1 : Maths.clamp(key.width() * RES / advance, 0.5, 2.0);
        g.translate(PAD * RES, (PAD + 7.5) * RES);
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(font);
        g.drawString(key.text(), 0, 0);
        g.dispose();
        byte[] pixels = ((java.awt.image.DataBufferByte) canvas.getRaster().getDataBuffer()).getData();
        float[] shape = new float[width * height];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = (pixels[i] & 0xFF) / 255f;
        }
        return shape;
    }

    private static Font awtFont(String name) {
        synchronized (FONTS) {
            return FONTS.computeIfAbsent(name, LyricGlow::loadFont);
        }
    }

    private static Font loadFont(String name) {
        Asset file = switch (name) {
            case "ShardClient" -> Asset.shared("font/inter_tight_semibold.ttf");
            case "Minecraft Uniform" -> null;
            default -> Asset.of("shardclient",
                    "font/lyrics/" + name.toLowerCase(Locale.ROOT).replace(' ', '_') + ".ttf");
        };
        if (file != null) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Assets.id(file));
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open()) {
                    return Font.createFont(Font.TRUETYPE_FONT, in);
                } catch (Exception ex) {
                    ShardClientRuntime.LOGGER.warn("Couldn't load {} for the lyrics glow", file, ex);
                }
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    }

    private static float[] blur(float[] source, int width, int height, float sigma) {
        int radius = Math.max(1, Math.round((float) (Math.sqrt(4 * sigma * sigma + 1) - 1) / 2));
        float[] a = source.clone();
        float[] b = new float[a.length];
        for (int pass = 0; pass < 3; pass++) {
            boxRows(a, b, width, height, radius);
            boxColumns(b, a, width, height, radius);
        }
        return a;
    }

    private static void boxRows(float[] in, float[] out, int width, int height, int radius) {
        float norm = 1f / (radius * 2 + 1);
        for (int y = 0; y < height; y++) {
            int row = y * width;
            float sum = 0;
            for (int x = -radius; x <= radius; x++) {
                sum += in[row + Maths.clamp(x, 0, width - 1)];
            }
            for (int x = 0; x < width; x++) {
                out[row + x] = sum * norm;
                sum += in[row + Math.min(width - 1, x + radius + 1)] - in[row + Math.max(0, x - radius)];
            }
        }
    }

    private static void boxColumns(float[] in, float[] out, int width, int height, int radius) {
        float norm = 1f / (radius * 2 + 1);
        for (int x = 0; x < width; x++) {
            float sum = 0;
            for (int y = -radius; y <= radius; y++) {
                sum += in[Maths.clamp(y, 0, height - 1) * width + x];
            }
            for (int y = 0; y < height; y++) {
                out[y * width + x] = sum * norm;
                sum += in[Math.min(height - 1, y + radius + 1) * width + x] - in[Math.max(0, y - radius) * width + x];
            }
        }
    }

    private static float max(float[] values) {
        float max = 0;
        for (float v : values) {
            max = Math.max(max, v);
        }
        return max;
    }
}
