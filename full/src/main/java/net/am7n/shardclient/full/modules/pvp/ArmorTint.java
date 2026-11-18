package net.am7n.shardclient.full.modules.pvp;

public final class ArmorTint {
    private static int tint = -1;

    private ArmorTint() {
    }

    public static void set(int argb) {
        tint = argb;
    }

    public static int apply(int color) {
        int current = tint;
        if (current == -1) {
            return color;
        }
        int a = (color >>> 24) & 0xFF;
        int r = ((color >> 16) & 0xFF) * ((current >> 16) & 0xFF) / 255;
        int g = ((color >> 8) & 0xFF) * ((current >> 8) & 0xFF) / 255;
        int b = (color & 0xFF) * (current & 0xFF) / 255;
        return a << 24 | r << 16 | g << 8 | b;
    }
}
