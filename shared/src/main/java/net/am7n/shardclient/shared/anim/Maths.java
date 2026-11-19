package net.am7n.shardclient.shared.anim;

public final class Maths {
    private Maths() {
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(long value, int min, int max) {
        return (int) Math.max(min, Math.min(max, value));
    }
}
