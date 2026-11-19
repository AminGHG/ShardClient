package net.am7n.shardclient.full.modules.lyrics;

import net.am7n.shardclient.full.platform.Images;
import net.am7n.shardclient.full.platform.Verts;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class VoxelFont {
    record Glyph(boolean[][] pixels, int width, int advance) {
    }

    private static final int ROWS = 8;
    private static final Glyph SPACE = new Glyph(new boolean[ROWS][0], 0, 4);
    private static Map<Integer, Glyph> glyphs;

    private VoxelFont() {
    }

    static Glyph glyph(int codepoint) {
        if (codepoint == ' ') {
            return SPACE;
        }
        if (glyphs == null) {
            glyphs = load();
        }
        return glyphs.get(codepoint);
    }

    static int width(String text, boolean bold) {
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Glyph glyph = glyph(cp);
            width += (glyph == null ? 6 : glyph.advance()) + (bold && glyph != SPACE ? 1 : 0);
        }
        return Math.max(0, width - 1);
    }

    static void draw(Verts out, String text, int[] colors, float x, float y,
                     float depth, boolean bold, int light) {
        float pen = x;
        int index = 0;
        for (int i = 0; i < text.length(); index++) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Glyph glyph = glyph(cp);
            int color = colors[Math.min(index, colors.length - 1)];
            if (glyph != null && glyph != SPACE) {
                boolean[][] px = bold ? embolden(glyph.pixels()) : glyph.pixels();
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < px[row].length; col++) {
                        if (px[row][col]) {
                            cube(out, px, row, col, pen + col, y + row, depth, color, light);
                        }
                    }
                }
            }
            pen += (glyph == null ? 6 : glyph.advance()) + (bold && glyph != SPACE ? 1 : 0);
        }
    }

    private static boolean[][] embolden(boolean[][] px) {
        boolean[][] out = new boolean[px.length][];
        for (int r = 0; r < px.length; r++) {
            out[r] = new boolean[px[r].length + 1];
            for (int c = 0; c < px[r].length; c++) {
                if (px[r][c]) {
                    out[r][c] = true;
                    out[r][c + 1] = true;
                }
            }
        }
        return out;
    }

    private static boolean lit(boolean[][] px, int row, int col) {
        return row >= 0 && row < px.length && col >= 0 && col < px[row].length && px[row][col];
    }

    private static void cube(Verts out, boolean[][] px, int row, int col, float x0,
                             float y0, float depth, int color, int light) {
        float x1 = x0 + 1;
        float y1 = y0 + 1;
        float zf = 0;
        float zb = -depth;
        quad(out, x0, y0, zf, x1, y0, zf, x1, y1, zf, x0, y1, zf, color, light);
        if (depth <= 0) {
            return;
        }
        quad(out, x0, y0, zb, x0, y1, zb, x1, y1, zb, x1, y0, zb, shade(color, 0.45f), light);
        if (!lit(px, row - 1, col)) {
            quad(out, x0, y0, zb, x1, y0, zb, x1, y0, zf, x0, y0, zf, shade(color, 0.82f), light);
        }
        if (!lit(px, row + 1, col)) {
            quad(out, x0, y1, zf, x1, y1, zf, x1, y1, zb, x0, y1, zb, shade(color, 0.5f), light);
        }
        if (!lit(px, row, col - 1)) {
            quad(out, x0, y0, zb, x0, y0, zf, x0, y1, zf, x0, y1, zb, shade(color, 0.66f), light);
        }
        if (!lit(px, row, col + 1)) {
            quad(out, x1, y0, zf, x1, y0, zb, x1, y1, zb, x1, y1, zf, shade(color, 0.6f), light);
        }
    }

    private static void quad(Verts out, float ax, float ay, float az, float bx, float by,
                             float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color, int light) {
        out.color(ax, ay, az, color, light);
        out.color(bx, by, bz, color, light);
        out.color(cx, cy, cz, color, light);
        out.color(dx, dy, dz, color, light);
        out.color(dx, dy, dz, color, light);
        out.color(cx, cy, cz, color, light);
        out.color(bx, by, bz, color, light);
        out.color(ax, ay, az, color, light);
    }

    private static int shade(int argb, float factor) {
        int r = Math.round((argb >> 16 & 0xFF) * factor);
        int g = Math.round((argb >> 8 & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static Map<Integer, Glyph> load() {
        Map<Integer, Glyph> out = new HashMap<>();
        try {
            readProviders(Asset.minecraft("font/include/default.json"), out);
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.warn("Could not read Minecraft's font for 3D lyrics", ex);
        }
        return out;
    }

    private static void readProviders(Asset file, Map<Integer, Glyph> out) throws Exception {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Assets.id(file));
        if (resource.isEmpty()) {
            return;
        }
        JsonObject json;
        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (JsonElement element : json.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            if ("bitmap".equals(provider.get("type").getAsString())) {
                readBitmap(provider, out);
            }
        }
    }

    private static void readBitmap(JsonObject provider, Map<Integer, Glyph> out) throws Exception {
        Asset file = Asset.parse(provider.get("file").getAsString());
        Asset texture = Asset.of(file.namespace(), "textures/" + file.path());
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Assets.id(texture));
        if (resource.isEmpty()) {
            return;
        }
        JsonArray rows = provider.getAsJsonArray("chars");
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
            int cellWidth = image.getWidth() / columns;
            int cellHeight = image.getHeight() / rows.size();
            for (int r = 0; r < rows.size(); r++) {
                String row = rows.get(r).getAsString();
                int c = 0;
                for (int i = 0; i < row.length(); c++) {
                    int cp = row.codePointAt(i);
                    i += Character.charCount(cp);
                    if (cp == 0 || out.containsKey(cp)) {
                        continue;
                    }
                    Glyph glyph = readGlyph(image, c * cellWidth, r * cellHeight, cellWidth, cellHeight);
                    if (glyph != null) {
                        out.put(cp, glyph);
                    }
                }
            }
        }
    }

    private static Glyph readGlyph(NativeImage image, int x0, int y0, int cellWidth, int cellHeight) {
        float step = cellHeight / (float) ROWS;
        int columns = Math.max(1, Math.round(cellWidth / step));
        boolean[][] px = new boolean[ROWS][columns];
        int width = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < columns; col++) {
                int sx = x0 + Math.min(cellWidth - 1, Math.round((col + 0.5f) * step - 0.5f));
                int sy = y0 + Math.min(cellHeight - 1, Math.round((row + 0.5f) * step - 0.5f));
                int alpha = Images.alpha(image, sx, sy);
                if (alpha > 127) {
                    px[row][col] = true;
                    width = Math.max(width, col + 1);
                }
            }
        }
        if (width == 0) {
            return null;
        }
        boolean[][] trimmed = new boolean[ROWS][width];
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(px[row], 0, trimmed[row], 0, width);
        }
        return new Glyph(trimmed, width, width + 1);
    }
}
