package net.am7n.shardclient.full.modules.lyrics;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.full.media.Lyrics;
import net.am7n.shardclient.full.media.MediaWatcher;
import net.am7n.shardclient.full.media.NowPlaying;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LyricsModule extends HudModule {
    public final ChoiceSetting preset = choice("preset", "Preset", "Custom", "Custom", "Starry Sky", "Solid Blocks",
            "Neon Sign", "Karaoke", "Poster");
    private String appliedPreset;

    public final ChoiceSetting place = choice("place", "Where", "In front", "Screen", "In front", "Sky", "Around you",
            "Follow view");
    final NumberSetting distance = number("distance", "World distance", 6, 2, 30, 0.5, " blocks");
    final NumberSetting height = number("height", "World height", 0.4, -4, 12, 0.1, " blocks");
    final NumberSetting sideSpread = number("side_spread", "Side spread", 14, 0, 40, 1, "°");
    final NumberSetting skyDistance = number("sky_distance_v2", "Sky distance", 80, 30, 200, 5, " blocks");
    final NumberSetting skyHeight = number("sky_height_v2", "Sky height", 32, 10, 100, 1, " blocks");
    final NumberSetting worldSize = number("world_size", "World text size", 1, 0.25, 4, 0.05, "×");

    final BooleanSetting facePlayer = bool("face_you", "Turn to face you", false);
    final ChoiceSetting letters = choice("letters", "Letters", "3D", "3D", "Flat");
    final NumberSetting depth = number("depth", "3D depth", 2, 0.5, 6, 0.5, " px");
    final BooleanSetting throughWalls = bool("through_walls", "See through walls", true);

    final ChoiceSetting font = choice("font", "Font", "Minecraft", "Minecraft", "Minecraft Uniform", "ShardClient",
            "Caveat", "Dancing Script", "Satisfy", "Permanent Marker", "Bebas Neue", "Anton", "Righteous",
            "Space Mono", "Random per song", "Random per line");
    final BooleanSetting bold = bool("bold", "Bold", false);
    final BooleanSetting uppercase = bool("uppercase", "Uppercase", false);
    final ChoiceSetting color = choice("color", "Colour", "White", "White", "Lilac", "Pink", "Mint", "Sky", "Gold",
            "Gradient", "Rainbow");
    final BooleanSetting shadow = bool("shadow", "Shadow", true);
    final BooleanSetting outline = bool("outline", "Outline", false);
    final ChoiceSetting glow = choice("glow_v2", "Glow", "Off", "Off", "Soft", "Strong");
    final ChoiceSetting glowColor = choice("glow_color", "Glow colour", "Same as text", "Same as text", "White",
            "Lilac", "Pink", "Gold");

    final ChoiceSetting enter = choice("enter", "Entrance", "Rise", "Fade", "Rise", "Drop", "Pop", "Typewriter",
            "Words");
    final ChoiceSetting exit = choice("exit", "Exit", "Fade", "Fade", "Rise", "Drop", "Shrink", "Backspace", "None");
    final NumberSetting speed = number("anim_speed", "Animation speed", 1, 0.25, 3, 0.05, "×");
    final ChoiceSetting idle = choice("idle", "While shown", "None", "None", "Float", "Pulse", "Wave");
    final BooleanSetting karaoke = bool("karaoke", "Karaoke fill", false);
    final BooleanSetting next = bool("next", "Show the next line", true);
    final BooleanSetting previous = bool("previous", "Keep the last line briefly", true);

    final NumberSetting offset = number("offset", "Timing offset", 0, -2000, 2000, 50, " ms");
    final BooleanSetting spotifyOnly = bool("spotify_only", "Only Spotify", false);
    final BooleanSetting whilePaused = bool("paused", "Keep showing while paused", false);

    private Lyrics shownLyrics;
    private int index = -2;
    private int lastIndex = -2;
    private long changedAt;

    public LyricsModule() {
        super("lyrics", "Lyrics", "Synced lyrics of your music, on screen or out in the world.", 0.5f, 0.2f);
        background.set(false);
    }

    @Override
    public void tick() {
        MediaWatcher.get().keepAlive(spotifyOnly.on());

        if (appliedPreset == null) {
            appliedPreset = preset.get();
        } else if (!appliedPreset.equals(preset.get())) {
            appliedPreset = preset.get();
            applyPreset(appliedPreset);
        }
    }

    private void applyPreset(String name) {
        switch (name) {
            case "Starry Sky" -> {
                place.set("Sky");
                skyDistance.set(90.0);
                skyHeight.set(38.0);
                worldSize.set(1.6);
                letters.set("Flat");
                font.set("Caveat");
                color.set("White");
                shadow.set(false);
                glow.set("Soft");
                glowColor.set("White");
                enter.set("Rise");
                exit.set("Fade");
                idle.set("Float");
                next.set(true);
            }
            case "Solid Blocks" -> {
                place.set("In front");
                distance.set(6.0);
                sideSpread.set(14.0);
                worldSize.set(1.0);
                letters.set("3D");
                depth.set(2.5);
                font.set("Minecraft");
                bold.set(true);
                color.set("White");
                glow.set("Off");
                enter.set("Pop");
                exit.set("Drop");
                idle.set("None");
            }
            case "Neon Sign" -> {
                place.set("In front");
                letters.set("Flat");
                font.set("Righteous");
                bold.set(false);
                color.set("Gradient");
                glow.set("Strong");
                glowColor.set("Same as text");
                throughWalls.set(true);
                enter.set("Fade");
                exit.set("Fade");
                idle.set("Pulse");
            }
            case "Karaoke" -> {
                place.set("Screen");
                font.set("ShardClient");
                color.set("White");
                karaoke.set(true);
                next.set(true);
                shadow.set(true);
                glow.set("Off");
                enter.set("Fade");
                exit.set("Rise");
                idle.set("None");
            }
            case "Poster" -> {
                place.set("Around you");
                worldSize.set(1.4);
                letters.set("Flat");
                font.set("Bebas Neue");
                uppercase.set(true);
                color.set("White");
                shadow.set(true);
                glow.set("Off");
                enter.set("Drop");
                exit.set("Shrink");
                idle.set("None");
            }
            default -> {
            }
        }
    }

    boolean inWorld() {
        return !place.is("Screen");
    }

    Optional<NowPlaying> track() {
        return MediaWatcher.get().playing().filter(t -> t.playing() || whilePaused.on());
    }

    List<LyricFrame> frames() {
        List<LyricFrame> out = new ArrayList<>();
        Lyrics lyrics = MediaWatcher.get().lyrics();
        NowPlaying playing = track().orElse(null);
        if (playing == null || lyrics.isEmpty()) {
            return out;
        }
        if (lyrics != shownLyrics) {
            shownLyrics = lyrics;
            index = -2;
            lastIndex = -2;
        }
        long position = playing.positionNow() + offset.intValue();
        int now = lyrics.indexAt(position);
        if (now != index) {
            lastIndex = index;
            index = now;
            changedAt = System.currentTimeMillis();
        }
        float since = (System.currentTimeMillis() - changedAt) / (1000f / speed.floatValue());
        float in = Math.clamp(since / 0.45f, 0f, 1f);
        float out_ = Math.clamp(since / 0.35f, 0f, 1f);

        if (previous.on() && lastIndex >= 0 && lastIndex < lyrics.lines().size() && lastIndex != index && out_ < 1
                && !exit.is("None")) {
            out.add(leaving(lyrics, lastIndex, out_));
        }
        if (index >= 0) {
            out.add(arriving(lyrics, index, in, position));
        }
        if (next.on() && index + 1 < lyrics.lines().size()) {
            String text = shownText(lyrics, index + 1);
            out.add(new LyricFrame(style(text, colorFor(index + 1, 0)), colorFor(index + 1, 0), 0.45f * in,
                    lineGap() + (1 - in) * 6, 0.8f, LyricFrame.Role.NEXT, index + 1));
        }
        return out;
    }

    private float lineGap() {
        return 13;
    }

    private LyricFrame arriving(Lyrics lyrics, int at, float t, long position) {
        String text = shownText(lyrics, at);
        float eased = (float) Easing.OUT_CUBIC.apply(t);
        float alpha = eased;
        float dy = 0;
        float scale = 1;
        switch (enter.get()) {
            case "Rise" -> dy = (1 - eased) * 10;
            case "Drop" -> dy = -(1 - eased) * 10;
            case "Pop" -> scale = 0.6f + 0.4f * backOut(t);
            case "Typewriter" -> {
                text = text.substring(0, Math.round(text.length() * Math.min(1f, t * 1.4f)));
                alpha = 1;
            }
            case "Words" -> {
                String[] words = text.split(" ");
                int shown = Math.max(1, Math.round(words.length * Math.min(1f, t * 1.2f)));
                text = String.join(" ", java.util.Arrays.copyOf(words, Math.min(shown, words.length)));
                alpha = 1;
            }
            default -> {
            }
        }
        long sinceStart = System.currentTimeMillis() - changedAt;
        switch (idle.get()) {
            case "Float" -> dy += (float) Math.sin(sinceStart / 600.0) * 1.5f;
            case "Pulse" -> scale *= 1 + 0.03f * (float) Math.sin(sinceStart / 300.0);
            default -> {
            }
        }
        int c = colorFor(at, sinceStart);
        Component styled = style(text, c);
        if (karaoke.on()) {
            styled = karaokeFill(text, c, fill(lyrics, at, position));
        } else if (idle.is("Wave")) {
            styled = wave(text, c, sinceStart);
        }
        return new LyricFrame(styled, c, alpha, dy, scale, LyricFrame.Role.CURRENT, at);
    }

    private LyricFrame leaving(Lyrics lyrics, int at, float t) {
        String text = shownText(lyrics, at);
        float eased = (float) Easing.OUT_CUBIC.apply(t);
        float alpha = 1 - eased;
        float dy = 0;
        float scale = 1;
        switch (exit.get()) {
            case "Rise" -> dy = -eased * 12;
            case "Drop" -> dy = eased * 12;
            case "Shrink" -> scale = 1 - 0.4f * eased;
            case "Backspace" -> {
                text = text.substring(0, Math.round(text.length() * (1 - t)));
                alpha = 1;
            }
            default -> {
            }
        }
        int c = colorFor(at, 0);
        return new LyricFrame(style(text, c), c, alpha * 0.7f, dy - lineGap() * (inWorld() ? 0 : 0), scale,
                LyricFrame.Role.PREVIOUS, at);
    }

    private static float backOut(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private static float fill(Lyrics lyrics, int at, long position) {
        long start = lyrics.lines().get(at).startMillis();
        long end = at + 1 < lyrics.lines().size() ? lyrics.lines().get(at + 1).startMillis() : start + 4000;
        return Math.clamp((position - start) / (float) Math.max(1, (end - start) * 0.85f), 0f, 1f);
    }

    private String shownText(Lyrics lyrics, int at) {
        String text = lyrics.lines().get(at).text();
        if (text.isBlank()) {
            text = "♪";
        }
        return uppercase.on() ? text.toUpperCase(Locale.ROOT) : text;
    }

    private static final int[] PALETTE = {0xFFFFFFFF, 0xFFC9A6FF, 0xFFFF9EC7, 0xFF8CF5C2, 0xFF8FD3FF, 0xFFFFD27A};

    int colorFor(int line, long sinceStart) {
        return switch (color.get()) {
            case "Lilac" -> PALETTE[1];
            case "Pink" -> PALETTE[2];
            case "Mint" -> PALETTE[3];
            case "Sky" -> PALETTE[4];
            case "Gold" -> PALETTE[5];
            case "Rainbow" -> PALETTE[1 + Math.floorMod(line, PALETTE.length - 1)];
            default -> PALETTE[0];
        };
    }

    int glowColor(int textColor) {
        return switch (glowColor.get()) {
            case "White" -> 0xFFFFFFFF;
            case "Lilac" -> PALETTE[1];
            case "Pink" -> PALETTE[2];
            case "Gold" -> PALETTE[5];
            default -> textColor;
        };
    }

    float glowStrength() {
        return switch (glow.get()) {
            case "Soft" -> 0.18f;
            case "Strong" -> 0.34f;
            default -> 0f;
        };
    }

    private static final List<String> RANDOM_FONTS = List.of("Minecraft", "Caveat", "Dancing Script", "Satisfy",
            "Permanent Marker", "Bebas Neue", "Anton", "Righteous", "Space Mono");

    private int styleLine;

    String fontFor(int line) {
        String chosen = font.get();
        if (!chosen.startsWith("Random")) {
            return chosen;
        }
        String song = MediaWatcher.get().playing().map(t -> t.artist() + "/" + t.title()).orElse("");
        int seed = chosen.equals("Random per line") ? (song + "#" + line).hashCode() : song.hashCode();
        return RANDOM_FONTS.get(Math.floorMod(seed * 0x9E3779B9, RANDOM_FONTS.size()));
    }

    boolean pixelFont(int line) {
        return fontFor(line).equals("Minecraft");
    }

    private Style baseStyle() {
        Style style = Style.EMPTY.withBold(bold.on());
        String chosen = fontFor(styleLine);
        if (chosen.equals("Minecraft Uniform")) {
            style = style.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("uniform")));
        } else if (!chosen.equals("Minecraft") && !chosen.equals("ShardClient")) {
            String id = "lyrics_" + chosen.toLowerCase(Locale.ROOT).replace(' ', '_');
            style = style.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient", id)));
        }
        return style;
    }

    Component style(String text, int c) {
        if (color.is("Gradient")) {
            MutableComponent out = Component.empty();
            for (int i = 0; i < text.length(); i++) {
                float t = text.length() <= 1 ? 0 : i / (float) (text.length() - 1);
                out.append(Component.literal(String.valueOf(text.charAt(i)))
                        .withStyle(baseStyle().withColor(lerp(PALETTE[1], PALETTE[2], t) & 0xFFFFFF)));
            }
            return finish(out);
        }
        return finish(Component.literal(text).withStyle(baseStyle().withColor(c & 0xFFFFFF)));
    }

    private Component finish(MutableComponent text) {
        return fontFor(styleLine).equals("ShardClient") ? Theme.text(text, bold.on()) : text;
    }

    private Component karaokeFill(String text, int c, float fill) {
        int split = Math.round(text.length() * fill);
        int dim = lerp(c, 0xFF000000, 0.55f);
        return finish(Component.empty()
                .append(Component.literal(text.substring(0, split)).withStyle(baseStyle().withColor(c & 0xFFFFFF)))
                .append(Component.literal(text.substring(split)).withStyle(baseStyle().withColor(dim & 0xFFFFFF))));
    }

    private Component wave(String text, int c, long sinceStart) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float light = 0.75f + 0.25f * (float) Math.sin(sinceStart / 180.0 - i * 0.45);
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(baseStyle().withColor(lerp(0xFF000000, c, light) & 0xFFFFFF)));
        }
        return finish(out);
    }

    static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8
                | Math.round(ab + (bb - ab) * t);
    }

    private static final int WIDTH = 260;

    @Override
    public boolean visible() {
        return !inWorld() && track().isPresent() && !MediaWatcher.get().lyrics().isEmpty();
    }

    @Override
    public Size size(Canvas canvas) {
        return new Size(WIDTH, 44);
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        List<LyricFrame> frames = frames();
        if (frames.isEmpty()) {
            frames = List.of(new LyricFrame(style("♪ Lyrics show here while music plays", PALETTE[0]), PALETTE[0], 1, 0,
                    1, LyricFrame.Role.CURRENT, 0));
        }
        Size size = size(canvas);
        drawBackground(canvas, x, y, size.width(), size.height());
        int centerX = x + WIDTH / 2;
        int baseY = y + 10;
        for (LyricFrame frame : frames) {
            float base = frame.role() == LyricFrame.Role.CURRENT ? 1.4f : frame.role() == LyricFrame.Role.NEXT ? 1f : 1.4f;
            float scale = base * frame.scale();
            int width = canvas.textWidth(frame.text());
            float fit = Math.min(1f, (WIDTH - 8) / Math.max(1f, width * scale));
            scale *= fit;
            float lineY = baseY + frame.offsetY() + (frame.role() == LyricFrame.Role.NEXT ? 10 : 0);
            canvas.push();
            canvas.translate(centerX, lineY);
            canvas.scale(scale, 0, 0);
            canvas.pushAlpha(frame.alpha());
            float glowAmount = glowStrength();
            if (glowAmount > 0 && frame.role() != LyricFrame.Role.NEXT) {
                int g = glowColor(frame.color());
                canvas.pushAlpha(glowAmount);
                for (int[] o : GLOW_OFFSETS) {
                    canvas.textCentered(recolor(frame.text(), g), o[0], o[1] - 4, g, false);
                }
                canvas.popAlpha();
            }
            canvas.textCentered(frame.text(), 0, -4, 0xFFFFFFFF, shadow.on());
            canvas.popAlpha();
            canvas.pop();
        }
    }

    static final int[][] GLOW_OFFSETS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1},
            {-2, 0}, {2, 0}, {0, -2}, {0, 2}};

    static Component recolor(Component text, int c) {
        MutableComponent out = Component.empty();
        text.visit((style, part) -> {
            out.append(Component.literal(part).withStyle(style.withColor(c & 0xFFFFFF)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
