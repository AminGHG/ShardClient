package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;

public final class Images {
    private Images() {
    }

    public static int alpha(NativeImage image, int x, int y) {
        return argb(image, x, y) >>> 24;
    }

    public static int argb(NativeImage image, int x, int y) {
        return image.getPixel(x, y);
    }

    public static void setArgb(NativeImage image, int x, int y, int argb) {
        image.setPixel(x, y, argb);
    }
}
