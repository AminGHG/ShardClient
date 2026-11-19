package net.am7n.shardclient.full.media;

import java.util.List;
import java.util.Optional;

final class MacMedia implements MediaSource {
    private static final long TIMEOUT_MS = 2000;

    private static final String RUNNING = String.join("\n",
            "set out to \"\"",
            "try",
            "if application id \"com.spotify.client\" is running then set out to out & \"spotify \"",
            "end try",
            "try",
            "if application id \"com.apple.Music\" is running then set out to out & \"music\"",
            "end try",
            "return out");

    private static final String SPOTIFY = String.join("\n",
            "tell application id \"com.spotify.client\"",
            "if player state is stopped then return \"\"",
            "set t to current track",
            "return \"Spotify\" & tab & (name of t) & tab & (artist of t) & tab & (album of t) & tab & ((duration of t) as integer) & tab & ((player position * 1000) as integer) & tab & (player state as text)",
            "end tell");

    private static final String MUSIC = String.join("\n",
            "tell application id \"com.apple.Music\"",
            "if player state is stopped then return \"\"",
            "set t to current track",
            "return \"Music\" & tab & (name of t) & tab & (artist of t) & tab & (album of t) & tab & (((duration of t) * 1000) as integer) & tab & ((player position * 1000) as integer) & tab & (player state as text)",
            "end tell");

    private MacMedia() {
    }

    static Optional<MediaSource> create() {
        return Shell.available("osascript") ? Optional.of(new MacMedia()) : Optional.empty();
    }

    @Override
    public Optional<NowPlaying> read() throws Exception {
        String running = Shell.run(List.of("osascript", "-e", RUNNING), TIMEOUT_MS);
        NowPlaying best = null;
        if (running.contains("spotify")) {
            best = parse(Shell.run(List.of("osascript", "-e", SPOTIFY), TIMEOUT_MS), System.currentTimeMillis());
        }
        if ((best == null || !best.playing()) && running.contains("music")) {
            NowPlaying music = parse(Shell.run(List.of("osascript", "-e", MUSIC), TIMEOUT_MS), System.currentTimeMillis());
            if (music != null && (best == null || music.playing())) {
                best = music;
            }
        }
        return Optional.ofNullable(best);
    }

    static NowPlaying parse(String line, long sampledAt) {
        String[] parts = line.strip().split("\t", -1);
        if (parts.length < 7 || parts[1].isBlank()) {
            return null;
        }
        return new NowPlaying(parts[0], parts[1], parts[2], parts[3], number(parts[4]), number(parts[5]), sampledAt,
                "playing".equals(parts[6].trim()));
    }

    private static long number(String value) {
        try {
            return Long.parseLong(value.trim().replace(",", "").replace(".", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
