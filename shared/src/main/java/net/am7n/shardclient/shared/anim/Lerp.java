package net.am7n.shardclient.shared.anim;

/** Mixing two values by a progress. */
public final class Lerp {

    private Lerp() {
    }

    public static float of(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static int round(float from, float to, float t) {
        return Math.round(of(from, to, t));
    }

    /** Mixes two ARGB colours channel by channel. */
    public static int color(int from, int to, float t) {
        float clamped = Math.clamp(t, 0f, 1f);
        int a = channel(from >>> 24, to >>> 24, clamped);
        int r = channel(from >> 16 & 0xFF, to >> 16 & 0xFF, clamped);
        int g = channel(from >> 8 & 0xFF, to >> 8 & 0xFF, clamped);
        int b = channel(from & 0xFF, to & 0xFF, clamped);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int channel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
