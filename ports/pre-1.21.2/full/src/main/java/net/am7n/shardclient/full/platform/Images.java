package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;

public final class Images {
    private Images() {
    }

    public static int alpha(NativeImage image, int x, int y) {
        return argb(image, x, y) >>> 24;
    }

    public static int argb(NativeImage image, int x, int y) {
        int abgr = image.getPixelRGBA(x, y);
        return abgr & 0xFF00FF00 | (abgr & 0xFF) << 16 | abgr >>> 16 & 0xFF;
    }

    public static void setArgb(NativeImage image, int x, int y, int argb) {
        image.setPixelRGBA(x, y, argb & 0xFF00FF00 | (argb & 0xFF) << 16 | argb >>> 16 & 0xFF);
    }
}
