package net.am7n.shardclient.full.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LinuxMedia implements MediaSource {
    private static final long TIMEOUT_MS = 1500;
    private static final long PLAYER_REFRESH_MS = 5000;
    private static final String PREFIX = "org.mpris.MediaPlayer2.";
    private static final Pattern NAME = Pattern.compile("string \"(org\\.mpris\\.MediaPlayer2\\.[^\"]+)\"");

    private final boolean dbusSend;
    private List<String> players = List.of();
    private long playersAt;
    private String lastKey = "";
    private long trackStartedAt;

    private LinuxMedia(boolean dbusSend) {
        this.dbusSend = dbusSend;
    }

    static Optional<MediaSource> create() {
        if (Shell.available("dbus-send")) {
            return Optional.of(new LinuxMedia(true));
        }
        if (Shell.available("playerctl")) {
            return Optional.of(new LinuxMedia(false));
        }
        return Optional.empty();
    }

    @Override
    public Optional<NowPlaying> read() throws Exception {
        Optional<NowPlaying> track = dbusSend ? readDbus() : readPlayerctl();
        return track.map(this::fixPosition);
    }

    private Optional<NowPlaying> readDbus() throws Exception {
        long now = System.currentTimeMillis();
        if (now - playersAt > PLAYER_REFRESH_MS) {
            String names = Shell.run(List.of("dbus-send", "--session", "--dest=org.freedesktop.DBus",
                    "--type=method_call", "--print-reply", "/org/freedesktop/DBus", "org.freedesktop.DBus.ListNames"),
                    TIMEOUT_MS);
            List<String> found = new ArrayList<>();
            Matcher matcher = NAME.matcher(names);
            while (matcher.find()) {
                found.add(matcher.group(1));
            }
            players = found;
            playersAt = now;
        }
        NowPlaying best = null;
        for (String player : players) {
            String reply;
            try {
                reply = Shell.run(List.of("dbus-send", "--session", "--dest=" + player, "--type=method_call",
                        "--print-reply", "/org/mpris/MediaPlayer2", "org.freedesktop.DBus.Properties.GetAll",
                        "string:org.mpris.MediaPlayer2.Player"), TIMEOUT_MS);
            } catch (java.io.IOException ex) {
                playersAt = 0;
                continue;
            }
            NowPlaying track = parseDbus(player.substring(PREFIX.length()), reply, System.currentTimeMillis());
            if (track != null && (best == null || track.playing() && !best.playing())) {
                best = track;
            }
        }
        return Optional.ofNullable(best);
    }

    static NowPlaying parseDbus(String app, String reply, long sampledAt) {
        String title = text(reply, "xesam:title");
        if (title == null || title.isBlank()) {
            return null;
        }
        String artist = first(reply, "xesam:artist");
        String album = text(reply, "xesam:album");
        String status = text(reply, "PlaybackStatus");
        long length = number(reply, "mpris:length") / 1000;
        long position = number(reply, "Position") / 1000;
        return new NowPlaying(app, title, artist == null ? "" : artist, album == null ? "" : album, length, position,
                sampledAt, "Playing".equals(status));
    }

    private static String text(String reply, String key) {
        Matcher matcher = Pattern.compile("string \"" + Pattern.quote(key) + "\"\\s*variant\\s+string \"(.*)\"\\s*$",
                Pattern.MULTILINE).matcher(reply);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String first(String reply, String key) {
        Matcher matcher = Pattern.compile("string \"" + Pattern.quote(key)
                + "\"\\s*variant\\s+array \\[\\s*string \"(.*)\"\\s*$", Pattern.MULTILINE).matcher(reply);
        return matcher.find() ? matcher.group(1) : text(reply, key);
    }

    private static long number(String reply, String key) {
        Matcher matcher = Pattern.compile("string \"" + Pattern.quote(key)
                + "\"\\s*variant\\s+(?:u?int64|u?int32|double) (-?[0-9]+)").matcher(reply);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
    }

    private Optional<NowPlaying> readPlayerctl() throws Exception {
        String line = Shell.run(List.of("playerctl", "metadata", "--format",
                "{{playerName}}\t{{status}}\t{{position}}\t{{mpris:length}}\t{{title}}\t{{artist}}\t{{album}}"),
                TIMEOUT_MS).strip();
        return Optional.ofNullable(parsePlayerctl(line, System.currentTimeMillis()));
    }

    static NowPlaying parsePlayerctl(String line, long sampledAt) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 7 || parts[4].isBlank()) {
            return null;
        }
        return new NowPlaying(parts[0], parts[4], parts[5], parts[6], parse(parts[3]) / 1000, parse(parts[2]) / 1000,
                sampledAt, "Playing".equals(parts[1]));
    }

    private static long parse(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private NowPlaying fixPosition(NowPlaying track) {
        String key = track.app() + "/" + track.artist() + "/" + track.title();
        long now = track.sampledAtMillis();
        if (!key.equals(lastKey)) {
            lastKey = key;
            trackStartedAt = now - track.positionMillis();
        }
        if (track.positionMillis() > 0 || !track.playing()) {
            trackStartedAt = now - track.positionMillis();
            return track;
        }
        return new NowPlaying(track.app(), track.title(), track.artist(), track.album(), track.durationMillis(),
                now - trackStartedAt, now, true);
    }
}
