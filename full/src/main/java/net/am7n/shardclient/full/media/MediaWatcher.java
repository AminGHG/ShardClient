package net.am7n.shardclient.full.media;

import net.am7n.shardclient.shared.ShardClientRuntime;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaWatcher {
    private static final long POLL_MS = 500;
    private static final long BACKOFF_MS = 10_000;

    private static final MediaWatcher INSTANCE = new MediaWatcher();

    private final Lrclib lrclib = new Lrclib();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean wanted;
    private volatile boolean onlySpotify;
    private volatile NowPlaying playing;
    private volatile Lyrics lyrics = Lyrics.NONE;
    private volatile boolean lyricsLoading;
    private volatile boolean available = true;

    private MediaWatcher() {
    }

    public static MediaWatcher get() {
        return INSTANCE;
    }

    private volatile long wantedAt;

    public void keepAlive(boolean spotifyOnly) {
        wantedAt = System.currentTimeMillis();
        wanted = true;
        onlySpotify = spotifyOnly;
        if (running.compareAndSet(false, true)) {
            Thread thread = new Thread(this::run, "ShardClient-Media");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private volatile boolean fixed;

    public void useForPreview(NowPlaying track, Lyrics lines) {
        fixed = true;
        playing = track;
        lyrics = lines;
    }

    public Optional<NowPlaying> playing() {
        return Optional.ofNullable(playing);
    }

    public Lyrics lyrics() {
        return lyrics;
    }

    public boolean lyricsLoading() {
        return lyricsLoading;
    }

    public boolean available() {
        return available;
    }

    private void run() {
        Optional<WindowsMedia> media = WindowsMedia.create();
        if (media.isEmpty()) {
            available = false;
            running.set(false);
            return;
        }
        WindowsMedia windows = media.get();
        try {
            while (wanted) {
                if (System.currentTimeMillis() - wantedAt > 3000) {
                    wanted = false;
                    break;
                }
                long wait = POLL_MS;
                if (fixed) {
                    Thread.sleep(wait);
                    continue;
                }
                try {
                    Optional<NowPlaying> now = windows.read();
                    NowPlaying track = now.filter(t -> !onlySpotify || t.fromSpotify()).orElse(null);
                    NowPlaying before = playing;
                    playing = track;
                    if (track == null) {
                        lyrics = Lyrics.NONE;
                    } else if (!track.sameTrack(before)) {
                        lyrics = Lyrics.NONE;
                        lyricsLoading = true;
                        try {
                            Lyrics found = lrclib.find(track);

                            if (track.sameTrack(playing)) {
                                lyrics = found;
                            }
                        } finally {
                            lyricsLoading = false;
                        }
                    }
                } catch (WindowsMedia.MediaException ex) {
                    ShardClientRuntime.LOGGER.debug("Now playing unavailable: {}", ex.getMessage());
                    playing = null;
                    wait = BACKOFF_MS;
                }
                Thread.sleep(wait);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Throwable ex) {
            ShardClientRuntime.LOGGER.warn("Now playing turned off after an error", ex);
            available = false;
        } finally {
            if (!fixed) {
                playing = null;
            }
            running.set(false);
        }
    }
}
