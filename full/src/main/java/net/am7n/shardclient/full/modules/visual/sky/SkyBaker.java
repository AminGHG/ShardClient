package net.am7n.shardclient.full.modules.visual.sky;

final class SkyBaker {
    private SkyBaker() {
    }

    private record Frame(double[] center, double[] right, double[] up, double halfWidth, double halfHeight) {
    }

    private static Frame frame(SkyPicture.Image image, float sizeDegrees, float directionDegrees, float heightDegrees) {
        double yaw = Math.toRadians(directionDegrees);
        double pitch = Math.toRadians(heightDegrees);
        double[] center = {Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch)};
        double[] right = {-center[2], 0, center[0]};
        double length = Math.sqrt(right[0] * right[0] + right[2] * right[2]);
        if (length < 1.0e-4) {
            right = new double[]{Math.cos(yaw), 0, Math.sin(yaw)};
        } else {
            right = new double[]{right[0] / length, 0, right[2] / length};
        }
        double[] up = {
                right[1] * center[2] - right[2] * center[1],
                right[2] * center[0] - right[0] * center[2],
                right[0] * center[1] - right[1] * center[0]
        };
        double half = Math.tan(Math.toRadians(Math.min(170, sizeDegrees)) / 2);
        double aspect = image.width() / (double) image.height();
        double halfWidth = aspect >= 1 ? half : half * aspect;
        double halfHeight = aspect >= 1 ? half / aspect : half;
        return new Frame(center, right, up, halfWidth, halfHeight);
    }

    static float[][] corners(SkyPicture.Image image, float size, float direction, float height, float radius) {
        Frame f = frame(image, size, direction, height);
        double distance = radius / Math.sqrt(1 + f.halfWidth * f.halfWidth + f.halfHeight * f.halfHeight);
        double[][] signs = {{-1, 1}, {-1, -1}, {1, -1}, {1, 1}};
        float[][] out = new float[4][3];
        for (int i = 0; i < 4; i++) {
            for (int axis = 0; axis < 3; axis++) {
                out[i][axis] = (float) (distance * (f.center[axis] + f.right[axis] * signs[i][0] * f.halfWidth
                        + f.up[axis] * signs[i][1] * f.halfHeight));
            }
        }
        return out;
    }

    static void stamp(int[] sky, int width, int height, SkyPicture.Image image, float size, float direction,
                      float pictureHeight, float opacity) {
        Frame f = frame(image, size, direction, pictureHeight);
        int[] src = image.argb();
        int iw = image.width();
        int ih = image.height();
        for (int y = 0; y < height; y++) {
            double lat = Math.PI / 2 - (y + 0.5) / height * Math.PI;
            double cl = Math.cos(lat);
            double sl = Math.sin(lat);
            for (int x = 0; x < width; x++) {
                double lon = (x + 0.5) / width * Math.PI * 2;
                double dx = cl * Math.cos(lon);
                double dz = cl * Math.sin(lon);
                double t = dx * f.center[0] + sl * f.center[1] + dz * f.center[2];
                if (t <= 1.0e-3) {
                    continue;
                }
                double px = (dx * f.right[0] + sl * f.right[1] + dz * f.right[2]) / t / f.halfWidth;
                double py = (dx * f.up[0] + sl * f.up[1] + dz * f.up[2]) / t / f.halfHeight;
                if (px < -1 || px > 1 || py < -1 || py > 1) {
                    continue;
                }
                double sx = (px + 1) / 2 * iw - 0.5;
                double sy = (1 - py) / 2 * ih - 0.5;
                int color = bilinear(src, iw, ih, sx, sy);
                double alpha = (color >>> 24) / 255.0 * opacity;
                if (alpha <= 0) {
                    continue;
                }
                int base = sky[y * width + x];
                int r = blend(base >> 16 & 0xFF, color >> 16 & 0xFF, alpha);
                int g = blend(base >> 8 & 0xFF, color >> 8 & 0xFF, alpha);
                int b = blend(base & 0xFF, color & 0xFF, alpha);
                sky[y * width + x] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }
    }

    static double coverage(SkyPicture.Image image, float size, float direction, float pictureHeight, float opacity,
                           double x, double y, double z) {
        Frame f = frame(image, size, direction, pictureHeight);
        double t = x * f.center[0] + y * f.center[1] + z * f.center[2];
        if (t <= 1.0e-3) {
            return 0;
        }
        double px = (x * f.right[0] + y * f.right[1] + z * f.right[2]) / t / f.halfWidth;
        double py = (x * f.up[0] + y * f.up[1] + z * f.up[2]) / t / f.halfHeight;
        if (px < -1 || px > 1 || py < -1 || py > 1) {
            return 0;
        }
        int color = bilinear(image.argb(), image.width(), image.height(), (px + 1) / 2 * image.width() - 0.5,
                (1 - py) / 2 * image.height() - 0.5);
        return (color >>> 24) / 255.0 * opacity;
    }

    private static int blend(int base, int over, double alpha) {
        return (int) Math.round(base + (over - base) * alpha);
    }

    private static int bilinear(int[] src, int w, int h, double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        int c00 = at(src, w, h, x0, y0);
        int c10 = at(src, w, h, x0 + 1, y0);
        int c01 = at(src, w, h, x0, y0 + 1);
        int c11 = at(src, w, h, x0 + 1, y0 + 1);
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            double top = (c00 >>> shift & 0xFF) * (1 - fx) + (c10 >>> shift & 0xFF) * fx;
            double bottom = (c01 >>> shift & 0xFF) * (1 - fx) + (c11 >>> shift & 0xFF) * fx;
            out |= (int) Math.round(top * (1 - fy) + bottom * fy) << shift;
        }
        return out;
    }

    private static int at(int[] src, int w, int h, int x, int y) {
        return src[Math.max(0, Math.min(h - 1, y)) * w + Math.max(0, Math.min(w - 1, x))];
    }
}
