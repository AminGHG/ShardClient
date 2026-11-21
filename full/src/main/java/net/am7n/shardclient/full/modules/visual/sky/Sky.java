package net.am7n.shardclient.full.modules.visual.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.platform.Chat;
import net.am7n.shardclient.full.platform.Client;
import net.am7n.shardclient.full.platform.Glow;
import net.am7n.shardclient.full.platform.Servers;
import net.am7n.shardclient.full.platform.Verts;
import net.am7n.shardclient.full.platform.World;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Size;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Sky extends Module {
    private static final int WIDTH = 4096;
    private static final int HEIGHT = 2048;
    private static final String NORMAL = "Normal";
    private static final int CACHE_VERSION = 2;
    private static final int STAR_TEXTURE = 8;
    private static final int THUMB_WIDTH = 192;
    private static final int THUMB_HEIGHT = 108;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ShardClient-Sky");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final ChoiceSetting sky = choice("sky", "Sky", SkyArt.Preset.NEBULA.label, names());
    private final NumberSetting brightness = number("brightness", "Brightness", 100, 20, 100, 5, "%");
    private final BooleanSetting dayNight = bool("day_night", "Follow day and night", true);
    private final BooleanSetting hideClouds = bool("hide_clouds", "Hide clouds", true);
    private final BooleanSetting hideFog = bool("hide_fog", "Hide distance fog", true);
    private final BooleanSetting showPicture = bool("picture", "Show picture", true);
    private final NumberSetting pictureSize = number("picture_size", "Picture size", 40, 5, 150, 1, "°");
    private final NumberSetting pictureDirection = number("picture_direction", "Picture direction", 0, 0, 359, 1, "°");
    private final NumberSetting pictureHeight = number("picture_height", "Picture height", 35, -10, 90, 1, "°");
    private final NumberSetting pictureOpacity = number("picture_opacity", "Picture opacity", 100, 10, 100, 5, "%");

    private volatile SkyPicture.Image picture;
    private volatile boolean pictureLoaded;
    private volatile String status = "";

    private String wantedKey = "";
    private volatile String bakingKey = "";
    private volatile Asset texture;
    private volatile List<SkyArt.Star> stars = List.of();
    private Asset starTexture;
    private Asset pictureTexture;
    private volatile int[][] thumbnails;
    private boolean thumbnailsQueued;
    private final Asset[] thumbnailAssets = new Asset[SkyArt.Preset.values().length + 1];
    private SkyPicture.Image uploadedPicture;
    private int counter;

    public Sky() {
        super("sky", "Sky", "Swap the sky for one of 10 skies or hang your own picture in it.", Category.VISUAL);
        sky.heading("Sky");
        sky.showIf(() -> false);
        showPicture.heading("Picture");
        hideClouds.showIf(() -> preset() != null);
        hideFog.showIf(() -> preset() != null);
        pictureSize.showIf(this::pictureVisible);
        pictureDirection.showIf(this::pictureVisible);
        pictureHeight.showIf(this::pictureVisible);
        pictureOpacity.showIf(this::pictureVisible);
    }

    private static String[] names() {
        String[] out = new String[SkyArt.Preset.values().length + 1];
        out[0] = NORMAL;
        for (SkyArt.Preset preset : SkyArt.Preset.values()) {
            out[preset.ordinal() + 1] = preset.label;
        }
        return out;
    }

    public boolean hidesClouds() {
        return isEnabled() && hideClouds.on() && preset() != null;
    }

    public boolean hidesFog() {
        if (!isEnabled() || !hideFog.on() || preset() == null || mc.level == null || mc.player == null
                || !Servers.overworldSky()) {
            return false;
        }
        return !mc.player.isEyeInFluid(FluidTags.WATER) && !mc.player.isEyeInFluid(FluidTags.LAVA)
                && !mc.player.isInPowderSnow && !mc.player.hasEffect(MobEffects.BLINDNESS)
                && !mc.player.hasEffect(MobEffects.DARKNESS);
    }

    private boolean pictureVisible() {
        return showPicture.on() && picture != null;
    }

    private SkyArt.Preset preset() {
        for (SkyArt.Preset preset : SkyArt.Preset.values()) {
            if (preset.label.equals(sky.get())) {
                return preset;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        if (!pictureLoaded) {
            pictureLoaded = true;
            WORKER.execute(() -> picture = SkyPicture.load());
        }
        if (!isEnabled()) {
            return;
        }
        SkyArt.Preset preset = preset();
        if (preset == null) {
            return;
        }
        String key = key(preset);
        if (!key.equals(wantedKey) && bakingKey.isEmpty()) {
            wantedKey = key;
            bakingKey = key;
            SkyPicture.Image image = showPicture.on() ? picture : null;
            float size = pictureSize.floatValue();
            float direction = pictureDirection.floatValue();
            float height = pictureHeight.floatValue();
            float opacity = pictureOpacity.floatValue() / 100f;
            WORKER.execute(() -> bake(key, preset, image, size, direction, height, opacity));
        }
    }

    private String key(SkyArt.Preset preset) {
        SkyPicture.Image image = showPicture.on() ? picture : null;
        return preset.name() + (image == null ? "" : "/" + System.identityHashCode(image) + "/" + pictureSize.intValue()
                + "/" + pictureDirection.intValue() + "/" + pictureHeight.intValue() + "/" + pictureOpacity.intValue());
    }

    private void bake(String key, SkyArt.Preset preset, SkyPicture.Image image, float size, float direction, float height,
                      float opacity) {
        try {
            int[] pixels = base(preset);
            if (image != null) {
                pixels = pixels.clone();
                SkyBaker.stamp(pixels, WIDTH, HEIGHT, image, size, direction, height, opacity);
            }
            List<SkyArt.Star> kept = new ArrayList<>();
            for (SkyArt.Star star : SkyArt.stars(preset)) {
                if (image == null || SkyBaker.coverage(image, size, direction, height, opacity, star.x(), star.y(),
                        star.z()) < 0.3) {
                    kept.add(star);
                }
            }
            int[] done = pixels;
            mc.execute(() -> {
                Asset next = Asset.of("shardclient", "sky/" + preset.name().toLowerCase(Locale.ROOT) + "_" + counter++);
                Glow.uploadColor(next, WIDTH, HEIGHT, done);
                Asset old = texture;
                texture = next;
                stars = List.copyOf(kept);
                if (old != null) {
                    Glow.release(old);
                }
            });
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.warn("Couldn't paint the {} sky", preset.label, ex);
        } finally {
            bakingKey = "";
        }
    }

    private static int[] base(SkyArt.Preset preset) throws Exception {
        Path folder = FabricLoader.getInstance().getConfigDir().resolve("shardclient").resolve("sky");
        String name = preset.name().toLowerCase(Locale.ROOT);
        Files.deleteIfExists(folder.resolve("cache-" + name + "-1.png"));
        Path file = folder.resolve("cache-" + name + "-" + CACHE_VERSION + ".png");
        if (Files.isRegularFile(file)) {
            BufferedImage cached = ImageIO.read(file.toFile());
            if (cached != null && cached.getWidth() == WIDTH && cached.getHeight() == HEIGHT) {
                return cached.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
            }
        }
        int[] pixels = SkyArt.paint(preset, WIDTH, HEIGHT);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        return pixels;
    }

    @Override
    protected void onDisable() {
        Asset old = texture;
        texture = null;
        stars = List.of();
        wantedKey = "";
        if (old != null) {
            Glow.release(old);
        }
    }

    public void render(World world) {
        if (!isEnabled() || mc.level == null || mc.player == null || !Servers.overworldSky()) {
            return;
        }
        if (mc.player.isEyeInFluid(FluidTags.WATER) || mc.player.isEyeInFluid(FluidTags.LAVA)) {
            return;
        }
        World.View view = world.view();
        if (view == null) {
            return;
        }
        float radius = Math.max(256, mc.options.getEffectiveRenderDistance() * 16 * 2f);
        float light = brightness.floatValue() / 100f * (dayNight.on() ? Client.daylight() : 1f);
        int shade = Math.round(light * 255);
        int argb = 0xFF000000 | shade << 16 | shade << 8 | shade;
        PoseStack pose = world.pose();
        SkyArt.Preset preset = preset();
        if (preset != null) {
            Asset current = texture;
            if (current != null) {
                world.sky(pose, current, out -> sphere(out, radius, argb));
                drawStars(world, pose, radius, light);
            }
            return;
        }
        drawPicture(world, pose, radius, light);
    }

    private void drawStars(World world, PoseStack pose, float radius, float light) {
        List<SkyArt.Star> current = stars;
        if (current.isEmpty() || light <= 0.02f) {
            return;
        }
        if (starTexture == null) {
            starTexture = Asset.of("shardclient", "sky/star");
            Glow.uploadColor(starTexture, STAR_TEXTURE, STAR_TEXTURE, starPixels());
        }
        float distance = radius * 0.99f;
        float shade = Math.min(1f, light);
        world.sky(pose, starTexture, out -> {
            for (SkyArt.Star star : current) {
                float x = star.x();
                float y = star.y();
                float z = star.z();
                float length = (float) Math.sqrt(x * x + z * z);
                float rx = length < 1.0e-3f ? 1 : -z / length;
                float rz = length < 1.0e-3f ? 0 : x / length;
                float ux = y * rz;
                float uy = z * rx - x * rz;
                float uz = -y * rx;
                float half = (float) (distance * Math.tan(Math.toRadians(star.size()) / 2));
                float cx = x * distance;
                float cy = y * distance;
                float cz = z * distance;
                int c = star.argb();
                int color = (c >>> 24) << 24 | Math.round((c >> 16 & 0xFF) * shade) << 16
                        | Math.round((c >> 8 & 0xFF) * shade) << 8 | Math.round((c & 0xFF) * shade);
                out.sky(cx + (-rx - ux) * half, cy - uy * half, cz + (-rz - uz) * half, 0, 0, color);
                out.sky(cx + (-rx + ux) * half, cy + uy * half, cz + (-rz + uz) * half, 0, 1, color);
                out.sky(cx + (rx + ux) * half, cy + uy * half, cz + (rz + uz) * half, 1, 1, color);
                out.sky(cx + (rx - ux) * half, cy - uy * half, cz + (rz - uz) * half, 1, 0, color);
            }
        });
    }

    private static int[] starPixels() {
        int[] pixels = new int[STAR_TEXTURE * STAR_TEXTURE];
        double middle = (STAR_TEXTURE - 1) / 2.0;
        for (int y = 0; y < STAR_TEXTURE; y++) {
            for (int x = 0; x < STAR_TEXTURE; x++) {
                double d = ((x - middle) * (x - middle) + (y - middle) * (y - middle)) / (middle * middle);
                int alpha = (int) Math.round(Math.exp(-d * 3.2) * 255);
                pixels[y * STAR_TEXTURE + x] = alpha << 24 | 0xFFFFFF;
            }
        }
        return pixels;
    }

    private void drawPicture(World world, PoseStack pose, float radius, float light) {
        SkyPicture.Image image = showPicture.on() ? picture : null;
        if (image != uploadedPicture) {
            if (pictureTexture != null) {
                Glow.release(pictureTexture);
                pictureTexture = null;
            }
            uploadedPicture = image;
            if (image != null) {
                pictureTexture = Asset.of("shardclient", "sky/picture_" + counter++);
                Glow.uploadColor(pictureTexture, image.width(), image.height(), image.argb());
            }
        }
        if (pictureTexture == null || image == null) {
            return;
        }
        int alpha = Math.round(pictureOpacity.floatValue() / 100f * 255);
        int shade = Math.round(Math.max(0.35f, light) * 255);
        int argb = alpha << 24 | shade << 16 | shade << 8 | shade;
        float[][] corners = SkyBaker.corners(image, pictureSize.floatValue(), pictureDirection.floatValue(),
                pictureHeight.floatValue(), radius * 0.98f);
        world.sky(pose, pictureTexture, out -> {
            out.sky(corners[0][0], corners[0][1], corners[0][2], 0, 0, argb);
            out.sky(corners[1][0], corners[1][1], corners[1][2], 0, 1, argb);
            out.sky(corners[2][0], corners[2][1], corners[2][2], 1, 1, argb);
            out.sky(corners[3][0], corners[3][1], corners[3][2], 1, 0, argb);
        });
    }

    private static void sphere(Verts out, float radius, int argb) {
        int rings = 32;
        int segments = 64;
        for (int ring = 0; ring < rings; ring++) {
            double lat0 = Math.PI / 2 - Math.PI * ring / rings;
            double lat1 = Math.PI / 2 - Math.PI * (ring + 1) / rings;
            float v0 = ring / (float) rings;
            float v1 = (ring + 1) / (float) rings;
            for (int seg = 0; seg < segments; seg++) {
                double lon0 = Math.PI * 2 * seg / segments;
                double lon1 = Math.PI * 2 * (seg + 1) / segments;
                float u0 = seg / (float) segments;
                float u1 = (seg + 1) / (float) segments;
                vertex(out, radius, lat0, lon0, u0, v0, argb);
                vertex(out, radius, lat1, lon0, u0, v1, argb);
                vertex(out, radius, lat1, lon1, u1, v1, argb);
                vertex(out, radius, lat0, lon1, u1, v0, argb);
            }
        }
    }

    private static void vertex(Verts out, float radius, double lat, double lon, float u, float v, int argb) {
        double c = Math.cos(lat);
        out.sky((float) (radius * c * Math.cos(lon)), (float) (radius * Math.sin(lat)), (float) (radius * c * Math.sin(lon)),
                u, v, argb);
    }

    @Override
    public Element preview() {
        queueThumbnails();
        return new Previews();
    }

    private void queueThumbnails() {
        if (thumbnailsQueued) {
            return;
        }
        thumbnailsQueued = true;
        WORKER.execute(() -> {
            try {
                String[] options = names();
                int[][] out = new int[options.length][];
                out[0] = SkyArt.normalThumbnail(THUMB_WIDTH, THUMB_HEIGHT);
                for (SkyArt.Preset preset : SkyArt.Preset.values()) {
                    out[preset.ordinal() + 1] = SkyArt.thumbnail(preset, THUMB_WIDTH, THUMB_HEIGHT);
                    thumbnails = out.clone();
                }
                thumbnails = out;
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.warn("Couldn't paint the sky previews", ex);
            }
        });
    }

    private Asset thumbnail(int index) {
        int[][] painted = thumbnails;
        if (painted == null || painted[index] == null) {
            return null;
        }
        if (thumbnailAssets[index] == null) {
            thumbnailAssets[index] = Asset.of("shardclient", "sky/thumbnail_" + index);
            Gfx.registerTexture(thumbnailAssets[index], THUMB_WIDTH, THUMB_HEIGHT, painted[index]);
        }
        return thumbnailAssets[index];
    }

    private final class Previews extends Container {
        private final Picker picker = new Picker();
        private final PictureRow pictureRow = new PictureRow();

        Previews() {
            add(picker);
            add(pictureRow);
        }

        @Override
        public Size measure() {
            return new Size(170, picker.heightFor(width) + 6 + pictureRow.measure().height());
        }

        @Override
        protected void layout() {
            int pickerHeight = picker.heightFor(width);
            picker.place(x, y, width, pickerHeight);
            pictureRow.place(x, y + pickerHeight + 6, width, pictureRow.measure().height());
        }
    }

    private final class Picker extends Element {
        private static final int GAP = 4;

        private int columns(int available) {
            return available >= 250 ? 4 : 3;
        }

        int heightFor(int available) {
            int room = available > 0 ? available : 280;
            int cols = columns(room);
            int tileWidth = (room - GAP * (cols - 1)) / cols;
            int rows = (names().length + cols - 1) / cols;
            return rows * (tileWidth * 9 / 16) + (rows - 1) * GAP;
        }

        private int tileWidth() {
            int cols = columns(width);
            return (width - GAP * (cols - 1)) / cols;
        }

        private int at(double mouseX, double mouseY) {
            int cols = columns(width);
            int tileWidth = tileWidth();
            int tileHeight = tileWidth * 9 / 16;
            String[] options = names();
            for (int i = 0; i < options.length; i++) {
                int tx = x + (i % cols) * (tileWidth + GAP);
                int ty = y + (i / cols) * (tileHeight + GAP);
                if (mouseX >= tx && mouseX < tx + tileWidth && mouseY >= ty && mouseY < ty + tileHeight) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public Size measure() {
            return new Size(170, heightFor(width));
        }

        @Override
        public void render(Canvas canvas) {
            int cols = columns(width);
            int tileWidth = tileWidth();
            int tileHeight = tileWidth * 9 / 16;
            String[] options = names();
            int hovered = at(canvas.mouseX(), canvas.mouseY());
            for (int i = 0; i < options.length; i++) {
                int tx = x + (i % cols) * (tileWidth + GAP);
                int ty = y + (i / cols) * (tileHeight + GAP);
                boolean selected = options[i].equals(sky.get());
                Asset image = thumbnail(i);
                if (image != null) {
                    canvas.gfx().blit(image, tx, ty, 0, 0, tileWidth, tileHeight, THUMB_WIDTH, THUMB_HEIGHT,
                            THUMB_WIDTH, THUMB_HEIGHT, canvas.tint(0xFFFFFFFF));
                } else {
                    canvas.fill(tx, ty, tileWidth, tileHeight, Theme.colors().inset());
                }
                int border = selected ? Theme.colors().accentStrong() : i == hovered ? 0xCCFFFFFF : 0x40FFFFFF;
                canvas.frame(tx, ty, tileWidth, tileHeight, border);
                if (selected) {
                    canvas.frame(tx + 1, ty + 1, tileWidth - 2, tileHeight - 2, border);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            int index = at(mouseX, mouseY);
            if (index < 0) {
                return false;
            }
            sky.set(names()[index]);
            return true;
        }
    }

    private void choosePicture() {
        status = "Choose a picture…";
        WORKER.execute(() -> {
            try {
                Path chosen = SkyPicture.choose();
                if (chosen == null) {
                    status = "";
                    return;
                }
                usePicture(chosen);
            } catch (Throwable ex) {
                status = "Couldn't open the file picker";
                ShardClientRuntime.LOGGER.warn("Sky picture picker failed", ex);
            }
        });
    }

    private void pastePicture() {
        String text = mc.keyboardHandler.getClipboard();
        status = "Pasting…";
        WORKER.execute(() -> {
            try {
                Path pasted = SkyPicture.pasted(text);
                if (pasted == null) {
                    status = "No picture on the clipboard";
                    return;
                }
                usePicture(pasted);
            } catch (Throwable ex) {
                status = "Couldn't read the clipboard";
                ShardClientRuntime.LOGGER.warn("Sky picture paste failed", ex);
            }
        });
    }

    private void usePicture(Path source) {
        try {
            picture = SkyPicture.store(source);
            showPicture.set(true);
            status = "Picture set";
            mc.execute(() -> Chat.actionBar(Component.literal("Sky picture set")));
        } catch (Exception ex) {
            status = "That file isn't a picture";
        }
    }

    private void removePicture() {
        WORKER.execute(() -> {
            try {
                SkyPicture.delete();
            } catch (Exception ignored) {
            }
            picture = null;
            status = "Picture removed";
        });
    }

    private final class PictureRow extends Container {
        private final Button choose = new Button("Choose picture", Sky.this::choosePicture);
        private final Button paste = new Button("Paste picture", Sky.this::pastePicture);
        private final Button remove = new Button("Remove", Sky.this::removePicture);

        PictureRow() {
            add(choose);
            add(paste);
            add(remove);
        }

        @Override
        public Size measure() {
            return new Size(170, 40);
        }

        @Override
        protected void layout() {
            int gap = 4;
            int each = (width - gap * 2) / 3;
            for (int i = 0; i < 3; i++) {
                Element button = children.get(i);
                Size size = button.measure();
                button.place(x + i * (each + gap), y + 14, each, size.height());
            }
        }

        @Override
        public void render(Canvas canvas) {
            String line = !status.isEmpty() ? status : picture != null
                    ? "Picture: " + picture.width() + "×" + picture.height() : "No picture yet";
            canvas.text(Theme.text(Component.literal(line), false), x, y + 2, Theme.colors().textMuted(), false);
            remove.visible(picture != null);
            super.render(canvas);
        }
    }
}
