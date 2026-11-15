package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.full.media.MediaWatcher;
import net.am7n.shardclient.full.media.NowPlaying;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class NowPlayingHud extends HudModule {
    private static final int WIDTH = 150;

    private final BooleanSetting spotifyOnly = bool("spotify_only", "Only Spotify", false);
    private final BooleanSetting whilePaused = bool("paused", "Keep showing while paused", true);

    public NowPlayingHud() {
        super("now_playing", "Now playing", "The song you are listening to, with its progress.", 1.0f, 0.92f);
    }

    @Override
    public void tick() {
        MediaWatcher.get().keepAlive(spotifyOnly.on());
    }

    private Optional<NowPlaying> track() {
        return MediaWatcher.get().playing().filter(t -> t.playing() || whilePaused.on());
    }

    @Override
    public boolean visible() {
        return track().isPresent();
    }

    @Override
    public Size size(Canvas canvas) {
        return new Size(WIDTH, PADDING * 2 + 32);
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        Palette colors = Theme.colors();
        Size size = size(canvas);
        drawBackground(canvas, x, y, size.width(), size.height());
        NowPlaying track = track().orElse(new NowPlaying("", "Song title", "Artist", "", 200_000, 64_000, 0, false));
        int inner = WIDTH - PADDING * 2;
        int textX = x + PADDING;
        canvas.text(Theme.text(Component.literal(clip(canvas, track.title(), inner, true)), true), textX, y + PADDING,
                colors.text(), false);
        String artist = track.playing() ? track.artist() : "Paused · " + track.artist();
        canvas.text(Theme.text(Component.literal(clip(canvas, artist, inner, false)), false), textX, y + PADDING + 11,
                colors.textMuted(), false);
        long position = track.positionNow();
        long duration = track.durationMillis();
        int barY = y + PADDING + 25;
        Theme.look().barTrack(canvas, textX, barY, inner - 58, 3);
        if (duration > 0) {
            Theme.look().barFill(canvas, textX, barY, Math.round((inner - 58) * (position / (float) duration)), 3,
                    colors.accentStrong());
        }
        Component time = Component.literal(clock(position) + " / " + clock(duration));
        canvas.textRight(time, x + WIDTH - PADDING, barY - 3, colors.textMuted(), false);
    }

    private static String clock(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String clip(Canvas canvas, String text, int max, boolean emphasis) {
        if (canvas.textWidth(Theme.text(Component.literal(text), emphasis)) <= max) {
            return text;
        }
        String out = text;
        while (!out.isEmpty() && canvas.textWidth(Theme.text(Component.literal(out + "…"), emphasis)) > max) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "…";
    }
}
