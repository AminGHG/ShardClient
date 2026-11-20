package net.am7n.shardclient.full.modules.visual.sky;

import java.util.Random;

public final class SkyArt {
    public enum Preset {
        NEBULA("Nebula"),
        GALAXY("Galaxy"),
        AURORA("Aurora"),
        SUNSET("Sunset"),
        DEEP_SPACE("Deep space"),
        BLOOD_MOON("Blood moon"),
        OCEAN_DUSK("Ocean dusk"),
        COTTON_CANDY("Cotton candy"),
        EMERALD("Emerald nebula"),
        VOID_STORM("Void storm");

        public final String label;

        Preset(String label) {
            this.label = label;
        }
    }

    private SkyArt() {
    }

    public static int[] paint(Preset preset, int width, int height) {
        int[] out = new int[width * height];
        Painter painter = new Painter(preset);
        for (int y = 0; y < height; y++) {
            double lat = Math.PI / 2 - (y + 0.5) / height * Math.PI;
            double cl = Math.cos(lat);
            double sl = Math.sin(lat);
            for (int x = 0; x < width; x++) {
                double lon = (x + 0.5) / width * Math.PI * 2;
                double dx = cl * Math.cos(lon);
                double dz = cl * Math.sin(lon);
                out[y * width + x] = painter.color(dx, sl, dz);
            }
        }
        return out;
    }

    private static final class Painter {
        private final Preset preset;
        private final Noise noise;
        private final Noise stars;

        Painter(Preset preset) {
            this.preset = preset;
            this.noise = new Noise(preset.ordinal() * 7919L + 17);
            this.stars = new Noise(preset.ordinal() * 104729L + 3);
        }

        int color(double x, double y, double z) {
            double[] c = switch (preset) {
                case NEBULA -> nebula(x, y, z, rgb(0x0A0420), rgb(0x12082E), rgb(0x7B2CE6), rgb(0xFF4FA8), rgb(0x4C8DFF), 1.25, 1.2);
                case GALAXY -> galaxy(x, y, z);
                case AURORA -> aurora(x, y, z);
                case SUNSET -> sunset(x, y, z);
                case DEEP_SPACE -> nebula(x, y, z, rgb(0x010309), rgb(0x030716), rgb(0x1C3C8C), rgb(0x3D7BE0), rgb(0x6A3CC0), 0.6, 2.6);
                case BLOOD_MOON -> nebula(x, y, z, rgb(0x1A0406), rgb(0x2A060C), rgb(0xB0142A), rgb(0xFF6A3A), rgb(0x6A0A3A), 1.15, 0.9);
                case OCEAN_DUSK -> oceanDusk(x, y, z);
                case COTTON_CANDY -> cottonCandy(x, y, z);
                case EMERALD -> nebula(x, y, z, rgb(0x021010), rgb(0x041A1C), rgb(0x10A57E), rgb(0x6CFFD2), rgb(0x2A6ED0), 1.25, 1.2);
                case VOID_STORM -> voidStorm(x, y, z);
            };
            return 0xFF000000 | clamp(c[0]) << 16 | clamp(c[1]) << 8 | clamp(c[2]);
        }

        private double[] nebula(double x, double y, double z, double[] low, double[] high, double[] a, double[] b,
                                double[] accent, double amount, double starDensity) {
            double[] c = mix(low, high, smooth(-0.2, 0.9, y));
            double wx = x + 0.9 * noise.fbm(x * 1.3 + 5, y * 1.3, z * 1.3, 4);
            double wy = y + 0.9 * noise.fbm(x * 1.3, y * 1.3 + 9, z * 1.3, 4);
            double wz = z + 0.9 * noise.fbm(x * 1.3, y * 1.3, z * 1.3 + 13, 4);
            double cloud = noise.fbm(wx * 1.35, wy * 1.35, wz * 1.35, 6);
            double density = smooth(-0.12, 0.55, cloud) * amount;
            double hue = noise.fbm(wx * 0.9 + 21, wy * 0.9, wz * 0.9, 3) * 0.5 + 0.5;
            double[] tint = mix(a, b, smooth(0.3, 0.75, hue));
            tint = mix(tint, accent, smooth(0.62, 0.9, noise.fbm(wx * 2.6, wy * 2.6 + 31, wz * 2.6, 4) * 0.5 + 0.5) * 0.7);
            c = add(c, scale(tint, density * 0.95));
            double core = smooth(0.3, 0.8, cloud) * amount;
            c = add(c, scale(mix(tint, rgb(0xFFFFFF), 0.35), core * 0.35));
            c = scale(c, 1 - 0.55 * smooth(0.2, 0.9, noise.fbm(x * 4, y * 4 + 50, z * 4, 4)) * density);
            return add(c, starField(x, y, z, starDensity * (1 - density * 0.6), 1.0));
        }

        private double[] galaxy(double x, double y, double z) {
            double[] c = mix(rgb(0x02030A), rgb(0x060818), smooth(-0.3, 1, y));
            double tilt = 0.45;
            double band = y * Math.cos(tilt) - x * Math.sin(tilt);
            double width = 0.17 + 0.08 * noise.fbm(x * 2, y * 2, z * 2, 3);
            double inBand = Math.exp(-band * band / (width * width));
            double dust = smooth(0.1, 0.7, noise.fbm(x * 6, y * 6, z * 6, 6) * 0.5 + 0.5);
            double glow = inBand * (0.55 + 0.45 * noise.fbm(x * 3 + 7, y * 3, z * 3, 5));
            double core = Math.pow(Math.max(0, x * 0.8 + z * 0.6), 3);
            double[] bandColor = mix(mix(rgb(0x5A4CC8), rgb(0xFF8FC8), smooth(0.4, 0.95, inBand) * 0.4), rgb(0xFFE2B8), smooth(0.5, 1, inBand) * (0.4 + core));
            c = add(c, scale(bandColor, glow * (0.7 + core * 0.9)));
            c = scale(c, 1 - 0.85 * inBand * Math.pow(dust, 1.5));
            double haze = smooth(-0.2, 0.6, noise.fbm(x * 1.5 + 40, y * 1.5, z * 1.5, 4));
            c = add(c, scale(rgb(0x3A1C6A), haze * 0.18));
            return add(c, starField(x, y, z, 1.4 + inBand * 4, 1.0));
        }

        private double[] aurora(double x, double y, double z) {
            double[] c = mix(rgb(0x041018), rgb(0x020610), smooth(0, 0.9, y));
            double angle = Math.atan2(z, x);
            double curtain = 0;
            for (int i = 0; i < 3; i++) {
                double wave = Math.sin(angle * (3 + i * 2) + noise.fbm(x * 2 + i * 5, 0, z * 2, 3) * 3 + i * 1.7);
                double base = 0.12 + 0.1 * i + 0.08 * wave;
                double height = 0.18 + 0.12 * noise.fbm(x * 3 + i, y, z * 3, 3);
                double up = (y - base) / height;
                double shape = up > 0 ? Math.exp(-up * 2.2) : Math.exp(up * 14);
                double stripes = 0.55 + 0.45 * Math.sin(angle * 90 + noise.fbm(x * 8, y * 2, z * 8 + i, 3) * 12);
                curtain += Math.max(0, shape) * stripes * (0.8 - i * 0.18);
            }
            double[] green = rgb(0x3CFFB0);
            double[] purple = rgb(0xB04CFF);
            double[] ac = mix(green, purple, smooth(0.25, 0.6, y));
            c = add(c, scale(ac, curtain * 0.65));
            c = add(c, scale(rgb(0x0E4A40), smooth(-0.1, 0.25, -Math.abs(y - 0.05) + 0.25) * 0.25));
            return add(c, starField(x, y, z, 1.6 * (1 - Math.min(1, curtain)), 1.0));
        }

        private double[] sunset(double x, double y, double z) {
            double sun = Math.max(0, x * 0.94 + z * 0.0 + y * 0.34 - 0.0);
            double[] top = rgb(0x1E1450);
            double[] mid = rgb(0xFF4F78);
            double[] low = rgb(0xFF9A3C);
            double t = smooth(-0.1, 0.8, y);
            double[] c = t < 0.35 ? mix(low, mid, t / 0.35) : mix(mid, top, (t - 0.35) / 0.65);
            c = add(c, scale(rgb(0xFFD08A), Math.pow(sun, 18) * 0.9 + Math.pow(sun, 4) * 0.25));
            double cloud = smooth(0.15, 0.7, noise.fbm(x * 2.2, y * 7, z * 2.2, 6) * 0.5 + 0.5)
                    * smooth(-0.05, 0.12, y) * (1 - smooth(0.35, 0.7, y));
            double[] lit = mix(rgb(0x4A1A5A), rgb(0xFFB060), Math.pow(sun, 2));
            c = mix(c, lit, cloud * 0.9);
            if (y < 0) {
                c = mix(c, rgb(0x2A1430), smooth(0, -0.4, y));
            }
            return c;
        }

        private double[] oceanDusk(double x, double y, double z) {
            double[] c = mix(rgb(0x1F6E8C), rgb(0x0B1E3F), smooth(-0.05, 0.6, y));
            c = mix(c, rgb(0x050B1C), smooth(0.5, 1, y));
            double glow = Math.max(0, -x * 0.9 + y * 0.2);
            c = add(c, scale(rgb(0x48D1CC), Math.pow(glow, 6) * 0.35));
            double cloud = smooth(0.2, 0.75, noise.fbm(x * 2, y * 6, z * 2, 5) * 0.5 + 0.5)
                    * smooth(0.0, 0.15, y) * (1 - smooth(0.3, 0.6, y));
            c = mix(c, rgb(0x16405E), cloud * 0.6);
            return add(c, starField(x, y, z, 0.8 * smooth(0.25, 0.8, y), 1.0));
        }

        private double[] cottonCandy(double x, double y, double z) {
            double[] c = mix(rgb(0xFF8EC6), rgb(0x6FA8FF), smooth(-0.1, 0.8, y));
            double cloud = smooth(0.15, 0.6, noise.fbm(x * 1.6, y * 3, z * 1.6, 6) * 0.5 + 0.5)
                    * (1 - smooth(0.5, 0.95, y));
            double[] cloudColor = mix(rgb(0xFFFFFF), rgb(0xFFDDF0), noise.fbm(x * 3, y * 3, z * 3, 3) * 0.5 + 0.5);
            c = mix(c, cloudColor, cloud * 0.85);
            double shade = smooth(0.3, 0.8, noise.fbm(x * 3 + 9, y * 6, z * 3, 4) * 0.5 + 0.5) * cloud;
            return mix(c, rgb(0xC07CE0), shade * 0.5);
        }

        private double[] voidStorm(double x, double y, double z) {
            double[] c = mix(rgb(0x05020C), rgb(0x0E0520), smooth(-0.3, 1, y));
            double wx = x + 1.1 * noise.fbm(x * 1.4 + 3, y * 1.4, z * 1.4, 5);
            double wy = y + 1.1 * noise.fbm(x * 1.4, y * 1.4 + 3, z * 1.4, 5);
            double wz = z + 1.1 * noise.fbm(x * 1.4, y * 1.4, z * 1.4 + 3, 5);
            double swirl = noise.fbm(wx * 2.4, wy * 2.4, wz * 2.4, 6);
            double vein = 1 - Math.abs(noise.fbm(wx * 1.8 + 11, wy * 1.8, wz * 1.8, 5));
            vein = Math.pow(Math.max(0, vein), 26) * smooth(-0.2, 0.4, swirl);
            c = add(c, scale(rgb(0x4B1FA8), smooth(-0.1, 0.7, swirl) * 0.7));
            c = add(c, scale(rgb(0x5AD6FF), vein * 1.1));
            c = add(c, scale(rgb(0xFFFFFF), Math.pow(vein, 3) * 0.4));
            return add(c, starField(x, y, z, 0.7, 1.0));
        }

        private double[] starField(double x, double y, double z, double density, double brightness) {
            double total = 0;
            double[] tint = {0, 0, 0};
            for (int layer = 0; layer < 3; layer++) {
                double scale = 70 + layer * 85;
                double fx = x * scale;
                double fy = y * scale;
                double fz = z * scale;
                long cx = (long) Math.floor(fx);
                long cy = (long) Math.floor(fy);
                long cz = (long) Math.floor(fz);
                long h = hash(cx, cy, cz, layer);
                double chance = (h & 0xFFFF) / 65535.0;
                if (chance > 0.05 * density) {
                    continue;
                }
                double px = cx + ((h >>> 16) & 0xFF) / 255.0;
                double py = cy + ((h >>> 24) & 0xFF) / 255.0;
                double pz = cz + ((h >>> 32) & 0xFF) / 255.0;
                double d = Math.sqrt((fx - px) * (fx - px) + (fy - py) * (fy - py) + (fz - pz) * (fz - pz));
                double size = 0.12 + ((h >>> 40) & 0xFF) / 255.0 * (0.3 - layer * 0.07);
                double light = Math.max(0, 1 - d / size);
                light = light * light * (0.5 + ((h >>> 48) & 0xFF) / 255.0 * 0.8) * brightness;
                double temp = ((h >>> 56) & 0xFF) / 255.0;
                double[] col = temp < 0.2 ? rgb(0xFFD2A0) : temp > 0.85 ? rgb(0xA8C8FF) : rgb(0xFFFFFF);
                tint = add(tint, scale(col, light));
                total += light;
            }
            return total > 0 ? scale(tint, 2.2) : tint;
        }

        private static long hash(long x, long y, long z, int layer) {
            long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ layer * 0x27D4EB2F165667C5L;
            h ^= h >>> 33;
            h *= 0xFF51AFD7ED558CCDL;
            h ^= h >>> 33;
            h *= 0xC4CEB9FE1A85EC53L;
            h ^= h >>> 33;
            return h;
        }
    }

    private static final class Noise {
        private final int[] perm = new int[512];

        Noise(long seed) {
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) {
                p[i] = i;
            }
            Random random = new Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int t = p[i];
                p[i] = p[j];
                p[j] = t;
            }
            for (int i = 0; i < 512; i++) {
                perm[i] = p[i & 255];
            }
        }

        double fbm(double x, double y, double z, int octaves) {
            double sum = 0;
            double amp = 0.5;
            double norm = 0;
            for (int i = 0; i < octaves; i++) {
                sum += amp * noise(x, y, z);
                norm += amp;
                x *= 2.03;
                y *= 2.03;
                z *= 2.03;
                amp *= 0.5;
            }
            return sum / norm;
        }

        double noise(double x, double y, double z) {
            int xi = (int) Math.floor(x) & 255;
            int yi = (int) Math.floor(y) & 255;
            int zi = (int) Math.floor(z) & 255;
            double xf = x - Math.floor(x);
            double yf = y - Math.floor(y);
            double zf = z - Math.floor(z);
            double u = fade(xf);
            double v = fade(yf);
            double w = fade(zf);
            int a = perm[xi] + yi;
            int aa = perm[a] + zi;
            int ab = perm[a + 1] + zi;
            int b = perm[xi + 1] + yi;
            int ba = perm[b] + zi;
            int bb = perm[b + 1] + zi;
            return lerp(w,
                    lerp(v, lerp(u, grad(perm[aa], xf, yf, zf), grad(perm[ba], xf - 1, yf, zf)),
                            lerp(u, grad(perm[ab], xf, yf - 1, zf), grad(perm[bb], xf - 1, yf - 1, zf))),
                    lerp(v, lerp(u, grad(perm[aa + 1], xf, yf, zf - 1), grad(perm[ba + 1], xf - 1, yf, zf - 1)),
                            lerp(u, grad(perm[ab + 1], xf, yf - 1, zf - 1), grad(perm[bb + 1], xf - 1, yf - 1, zf - 1))));
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private static double lerp(double t, double a, double b) {
            return a + t * (b - a);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }

    private static double[] rgb(int c) {
        return new double[]{(c >> 16 & 0xFF) / 255.0, (c >> 8 & 0xFF) / 255.0, (c & 0xFF) / 255.0};
    }

    private static double[] mix(double[] a, double[] b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(double[] a, double s) {
        return new double[]{a[0] * s, a[1] * s, a[2] * s};
    }

    private static double smooth(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private static int clamp(double v) {
        double x = Math.max(0, v);
        double mapped = x <= 0.8 ? x : 0.8 + 0.2 * (1 - Math.exp(-(x - 0.8) * 5));
        return (int) Math.max(0, Math.min(255, Math.round(mapped * 255)));
    }
}
