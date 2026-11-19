package net.am7n.shardclient.full.media;

public record NowPlaying(String app, String title, String artist, String album, long durationMillis,
                         long positionMillis, long sampledAtMillis, boolean playing) {
    public long positionNow() {
        long position = playing ? positionMillis + (System.currentTimeMillis() - sampledAtMillis) : positionMillis;
        return durationMillis > 0 ? Math.max(0, Math.min(durationMillis, position)) : Math.max(0, position);
    }

    public boolean sameTrack(NowPlaying other) {
        return other != null && title.equals(other.title) && artist.equals(other.artist);
    }

    public boolean fromSpotify() {
        return app.toLowerCase().contains("spotify");
    }
}
