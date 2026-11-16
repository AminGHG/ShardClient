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
import net.am7n.shardclient.shared.ui.Element;
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

    final ChoiceSetting layout = choice("layout", "Layout", "Lines", "Lines", "Words");
    final NumberSetting maxWidth = number("max_width", "Max line width", 170, 80, 400, 10, " px");
    final BooleanSetting next = bool("next", "Show the next line", true);
    final BooleanSetting previous = bool("previous", "Keep the last line briefly", true);
    final BooleanSetting karaoke = bool("karaoke", "Karaoke fill", false);

    public final ChoiceSetting place = choice("place", "Where", "In front", "Screen", "In front", "Sky", "Around you",
            "Follow view");
    final NumberSetting distance = number("distance", "Distance", 6, 2, 30, 0.5, " blocks");
    final NumberSetting height = number("height", "Height", 0.4, -4, 12, 0.1, " blocks");
    final NumberSetting sideSpread = number("side_spread", "Side spread", 14, 0, 40, 1, "°");
    final NumberSetting skyDistance = number("sky_distance_v2", "Sky distance", 80, 30, 200, 5, " blocks");
    final NumberSetting skyHeight = number("sky_height_v2", "Sky height", 32, 10, 100, 1, " blocks");
    final NumberSetting worldSize = number("world_size", "Text size", 1, 0.25, 4, 0.05, "×");
    final ChoiceSetting letters = choice("letters", "Letters", "3D", "3D", "Flat");
    final NumberSetting depth = number("depth", "3D depth", 2, 0.5, 6, 0.5, " px");
    final BooleanSetting facePlayer = bool("face_you", "Turn to face you", false);
    final BooleanSetting throughWalls = bool("through_walls_v2", "See through walls", false);

    final ChoiceSetting enter = choice("enter", "Entrance", "Rise", "Fade", "Rise", "Drop", "Pop", "Typewriter",
            "Word by word");
    final ChoiceSetting exit = choice("exit", "Exit", "Fade", "Fade", "Rise", "Drop", "Shrink", "Backspace", "None");
    final NumberSetting speed = number("anim_speed", "Animation speed", 1, 0.25, 3, 0.05, "×");
    final ChoiceSetting idle = choice("idle", "While shown", "None", "None", "Float", "Pulse", "Wave");

    final NumberSetting offset = number("offset", "Timing offset", 0, -2000, 2000, 50, " ms");
    final BooleanSetting spotifyOnly = bool("spotify_only", "Only Spotify", false);
    final BooleanSetting whilePaused = bool("paused", "Keep showing while paused", false);

    private static final class Timeline {
        private Lyrics shown;
        private int index = -2;
        private int last = -2;
        private long changedAt;
    }

    private final Timeline live = new Timeline();
    private final Timeline sample = new Timeline();
    private final long sampleStart = System.currentTimeMillis();
    private static final Lyrics SAMPLE = new Lyrics(List.of(
            new Lyrics.Line(0, "Lights are falling from the sky"),
            new Lyrics.Line(2800, "We keep running through the night"),
            new Lyrics.Line(5600, "Sing it louder"),
            new Lyrics.Line(8400, "Every word is shining bright")));
    private static final long SAMPLE_LENGTH = 11200;

    public LyricsModule() {
        super("lyrics", "Lyrics", "Synced lyrics of your music, on screen or out in the world.", 0.5f, 0.2f);
        background.set(false);
        moveToEnd(scale, background, enter, exit, speed, idle, offset, spotifyOnly, whilePaused);

        preset.heading("Style");
        layout.heading("Layout");
        place.heading("Position");
        enter.heading("Animation");
        offset.heading("Music");

        glowColor.showIf(() -> !glow.is("Off"));
        outline.showIf(this::inWorld);
        distance.showIf(() -> inWorld() && !place.is("Sky"));
        height.showIf(this::inWorld);
        sideSpread.showIf(() -> place.is("In front") || place.is("Sky"));
        skyDistance.showIf(() -> place.is("Sky"));
        skyHeight.showIf(() -> place.is("Sky"));
        worldSize.showIf(this::inWorld);
        letters.showIf(this::inWorld);
        depth.showIf(() -> inWorld() && letters.is("3D"));
        facePlayer.showIf(() -> inWorld() && !place.is("Follow view"));
        throughWalls.showIf(this::inWorld);
        scale.showIf(() -> !inWorld());
        background.showIf(() -> !inWorld());
    }

    int maxLineWidth() {
        return maxWidth.intValue();
    }

    boolean wordsLayout() {
        return layout.is("Words");
    }

    @Override
    public void tick() {
        MediaWatcher.get().keepAlive(spotifyOnly.on());

        if (appliedPreset == null) {
            appliedPreset = preset.get();
        } else if (!appliedPreset.equals(preset.get())) {
            appliedPreset = preset.get();
            applyPreset(appliedPreset);
            presetSnapshot = preset.is("Custom") ? null : snapshot();
        } else if (presetSnapshot != null && !snapshot().equals(presetSnapshot)) {
            presetSnapshot = null;
            preset.set("Custom");
            appliedPreset = "Custom";
        }
    }

    private String presetSnapshot;

    private String snapshot() {
        StringBuilder out = new StringBuilder();
        for (net.am7n.shardclient.full.module.setting.Setting<?> setting : settings()) {
            if (setting != preset) {
                out.append(setting.id()).append('=').append(setting.get()).append(';');
            }
        }
        return out.toString();
    }

    private void applyPreset(String name) {
        switch (name) {
            case "Starry Sky" -> {
                place.set("Sky");
                skyDistance.set(90.0);
                skyHeight.set(38.0);
                worldSize.set(1.1);
                letters.set("Flat");
                font.set("Caveat");
                color.set("Gradient");
                shadow.set(true);
                throughWalls.set(false);
                glow.set("Soft");
                glowColor.set("Lilac");
                layout.set("Words");
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
                color.set("Gold");
                glow.set("Off");
                layout.set("Lines");
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
                throughWalls.set(false);
                layout.set("Lines");
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
                layout.set("Lines");
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
                color.set("Rainbow");
                shadow.set(true);
                throughWalls.set(false);
                glow.set("Off");
                layout.set("Words");
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
        Lyrics lyrics = MediaWatcher.get().lyrics();
        NowPlaying playing = track().orElse(null);
        if (playing == null || lyrics.isEmpty()) {
            return new ArrayList<>();
        }
        return frames(live, lyrics, playing.positionNow() + offset.intValue());
    }

    private List<LyricFrame> sampleFrames() {
        return frames(sample, SAMPLE, (System.currentTimeMillis() - sampleStart) % SAMPLE_LENGTH);
    }

    private List<LyricFrame> frames(Timeline timeline, Lyrics lyrics, long position) {
        List<LyricFrame> out = new ArrayList<>();
        if (lyrics != timeline.shown) {
            timeline.shown = lyrics;
            timeline.index = -2;
            timeline.last = -2;
        }
        int now = lyrics.indexAt(position);
        if (now != timeline.index) {
            timeline.last = timeline.index;
            timeline.index = now;
            timeline.changedAt = System.currentTimeMillis();
        }
        int index = timeline.index;
        int lastIndex = timeline.last;
        float since = (System.currentTimeMillis() - timeline.changedAt) / (1000f / speed.floatValue());
        float in = Math.clamp(since / 0.45f, 0f, 1f);
        float out_ = Math.clamp(since / 0.35f, 0f, 1f);

        if (previous.on() && lastIndex >= 0 && lastIndex < lyrics.lines().size() && lastIndex != index && out_ < 1
                && !exit.is("None")) {
            out.add(leaving(lyrics, lastIndex, out_));
        }
        if (index >= 0) {
            out.add(arriving(timeline, lyrics, index, in, position));
        }
        if (next.on() && index + 1 < lyrics.lines().size()) {
            styleLine = index + 1;
            String text = shownText(lyrics, index + 1);
            Component styled = style(text, colorFor(index + 1, 0));
            out.add(new LyricFrame(styled, styled, colorFor(index + 1, 0), 0.45f * in, lineGap() + (1 - in) * 6, 0.8f,
                    LyricFrame.Role.NEXT, index + 1));
        }
        return out;
    }

    private float lineGap() {
        return 13;
    }

    private LyricFrame arriving(Timeline timeline, Lyrics lyrics, int at, float t, long position) {
        styleLine = at;
        String full = shownText(lyrics, at);
        String text = full;
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
            case "Word by word" -> {
                String[] words = text.split(" ");
                int shown = Math.max(1, Math.round(words.length * Math.min(1f, t * 1.2f)));
                text = String.join(" ", java.util.Arrays.copyOf(words, Math.min(shown, words.length)));
                alpha = 1;
            }
            default -> {
            }
        }
        long sinceStart = System.currentTimeMillis() - timeline.changedAt;
        switch (idle.get()) {
            case "Float" -> dy += (float) Math.sin(sinceStart / 600.0) * 1.5f;
            case "Pulse" -> scale *= 1 + 0.03f * (float) Math.sin(sinceStart / 300.0);
            default -> {
            }
        }
        int c = colorFor(at, sinceStart);
        Component styled = style(text, c);
        Component styledFull = style(full, c);
        if (karaoke.on()) {
            styled = karaokeFill(text, c, fill(lyrics, at, position));
        } else if (idle.is("Wave")) {
            styled = wave(text, c, sinceStart);
        }
        return new LyricFrame(styled, styledFull, c, alpha, dy, scale, LyricFrame.Role.CURRENT, at);
    }

    private LyricFrame leaving(Lyrics lyrics, int at, float t) {
        styleLine = at;
        String full = shownText(lyrics, at);
        String text = full;
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
        return new LyricFrame(style(text, c), style(full, c), c, alpha * 0.7f, dy, scale, LyricFrame.Role.PREVIOUS, at);
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
            case "Soft" -> 0.45f;
            case "Strong" -> 0.8f;
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
    private static final float SCREEN_SCALE = 1.4f;

    @Override
    public boolean visible() {
        return !inWorld() && track().isPresent() && !MediaWatcher.get().lyrics().isEmpty();
    }

    @Override
    public Size size(Canvas canvas) {
        return new Size(WIDTH, 44 + Math.round((currentHeight(canvas, frames()) - LyricLayout.ROW) * SCREEN_SCALE));
    }

    private float currentHeight(Canvas canvas, List<LyricFrame> frames) {
        for (LyricFrame frame : frames) {
            if (frame.role() == LyricFrame.Role.CURRENT) {
                return LyricLayout.height(screenRows(canvas, frame, SCREEN_SCALE * frame.scale(), wordsLayout()));
            }
        }
        return LyricLayout.ROW;
    }

    private List<LyricLayout.Row> screenRows(Canvas canvas, LyricFrame frame, float scale, boolean words) {
        int wrap = Math.max(40, Math.min(maxLineWidth(), Math.round((WIDTH - 8) / scale)));
        return LyricLayout.rows(frame.full(), words, wrap, fontFor(frame.lineId()) + bold.on(),
                c -> canvas.font().width(c));
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        List<LyricFrame> frames = frames();
        if (frames.isEmpty()) {
            Component hint = style("♪ Lyrics show here while music plays", PALETTE[0]);
            frames = List.of(new LyricFrame(hint, hint, PALETTE[0], 1, 0, 1, LyricFrame.Role.CURRENT, 0));
        }
        Size size = size(canvas);
        drawBackground(canvas, x, y, size.width(), size.height());
        drawLines(canvas, frames, x + WIDTH / 2, y + 10);
    }

    private void drawLines(Canvas canvas, List<LyricFrame> frames, int centerX, float baseY) {
        float currentHeight = currentHeight(canvas, frames);
        for (LyricFrame frame : frames) {
            if (frame.role() == LyricFrame.Role.PREVIOUS && wordsLayout()) {
                continue;
            }
            boolean isNext = frame.role() == LyricFrame.Role.NEXT;
            float scale = (isNext ? 1f : SCREEN_SCALE) * frame.scale();
            List<LyricLayout.Row> rows = screenRows(canvas, frame, scale, wordsLayout() && !isNext);
            float lineY = baseY + frame.offsetY()
                    + (isNext ? 10 + (currentHeight - LyricLayout.ROW) * SCREEN_SCALE : 0);
            canvas.push();
            canvas.translate(centerX, lineY);
            canvas.scale(scale, 0, 0);
            canvas.pushAlpha(frame.alpha());
            float glowAmount = isNext ? 0 : glowStrength();
            float rowY = -4;
            for (LyricLayout.Row row : rows) {
                Component part = LyricLayout.slice(frame.text(), row.start(), row.end());
                int rowX = -row.width() / 2;
                canvas.push();
                canvas.translate(0, rowY);
                canvas.scale(row.scale(), 0, 0);
                if (glowAmount > 0) {
                    drawGlow(canvas, frame, row, part, rowX, glowAmount);
                }
                canvas.line(part.getVisualOrderText(), rowX, 0, 0xFFFFFFFF, shadow.on());
                canvas.pop();
                rowY += row.height();
            }
            canvas.popAlpha();
            canvas.pop();
        }
    }

    private void drawGlow(Canvas canvas, LyricFrame frame, LyricLayout.Row row, Component part, int rowX,
                          float amount) {
        int shown = Math.min(row.width(), canvas.font().width(part));
        if (shown <= 0) {
            return;
        }
        LyricGlow.Texture glowTexture = glowTexture(frame, row);
        if (glowTexture == null) {
            return;
        }
        int fullWidth = row.width() + LyricGlow.PAD * 2;
        int quadWidth = shown + LyricGlow.PAD * 2;
        int uWidth = Math.round(glowTexture.width() * quadWidth / (float) fullWidth);
        int[] colors = glowColors(frame.color());
        int rgb = lerp(colors[0], colors[1], 0.5f) & 0xFFFFFF;
        int alpha = Math.round(Math.min(1f, amount) * 255);
        canvas.texture(GlowPipelines.GUI, glowTexture.id(), rowX - LyricGlow.PAD, -LyricGlow.PAD, quadWidth,
                LyricGlow.QUAD_HEIGHT, 0, 0, uWidth, glowTexture.height(), glowTexture.width(),
                glowTexture.height(), alpha << 24 | rgb);
    }

    LyricGlow.Texture glowTexture(LyricFrame frame, LyricLayout.Row row) {
        String text = LyricLayout.plain(LyricLayout.slice(frame.full(), row.start(), row.end()));
        return LyricGlow.get(text, fontFor(frame.lineId()), bold.on(), row.width());
    }

    int[] glowColors(int textColor) {
        if (glowColor.is("Same as text") && color.is("Gradient")) {
            return new int[]{PALETTE[1], PALETTE[2]};
        }
        int c = glowColor(textColor);
        return new int[]{c, c};
    }

    @Override
    public Element preview() {
        return new Preview();
    }

    private final class Preview extends Element {
        @Override
        public Size measure() {
            return new Size(160, 84);
        }

        @Override
        public void render(Canvas canvas) {
            canvas.smoothRect(x, y, width, height, 5, 0xFF0E0B18);
            canvas.gradient(x + 1, y + height / 2, width - 2, height / 2 - 1, 0x00000000, 0x55302050);
            canvas.text(Theme.text(Component.literal("PREVIEW"), true), x + 6, y + 5, 0x88FFFFFF, false);
            canvas.text(Theme.text(Component.literal(place.get()), false),
                    x + width - 6 - canvas.textWidth(Theme.text(Component.literal(place.get()), false)), y + 5,
                    0x66FFFFFF, false);
            List<LyricFrame> frames = sampleFrames();
            float block = currentHeight(canvas, frames) * SCREEN_SCALE + (next.on() ? 12 : 0);
            float fit = Math.min(1f, Math.min((width - 12) / (float) WIDTH, (height - 22) / block));
            int centerX = x + width / 2;
            float centerY = y + 8 + (height - 8) / 2f;
            canvas.clip(x + 1, y + 1, width - 2, height - 2);
            canvas.push();
            canvas.scale(fit, centerX, centerY);
            drawLines(canvas, frames, centerX, centerY - block / 2 + 4 * SCREEN_SCALE);
            canvas.pop();
            canvas.unclip();
        }
    }
}
