package net.am7n.shardclient.full.media;

import java.util.Locale;
import java.util.Optional;

public interface MediaSource {
    Optional<NowPlaying> read() throws Exception;

    static Optional<MediaSource> forThisSystem() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return WindowsMedia.create().map(media -> media);
        }
        if (os.contains("mac")) {
            return MacMedia.create();
        }
        return LinuxMedia.create();
    }
}
