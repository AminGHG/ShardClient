package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class SkyCheck {
    private record Step(long delay, BooleanSupplier ready, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static long readyAt = -1;

    private SkyCheck() {
    }

    private static void after(long delay, Runnable action) {
        STEPS.add(new Step(delay, () -> true, action));
    }

    private static void when(BooleanSupplier ready, long delay, Runnable action) {
        STEPS.add(new Step(delay, ready, action));
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        when(() -> mc.gui.overlay() == null && mc.gui.screen() instanceof ShardTitleScreen, 300, () -> {
            mc.options.framerateLimit().set(120);
            module("motion_blur").setEnabled(false);
            set("sky", "day_night", false);
            module("sky").setEnabled(true);
            mc.createWorldOpenFlows().openWorld("Demo_World", () -> mc.gui.setScreen(new ShardTitleScreen()));
        });
        when(() -> mc.level != null && mc.player != null && mc.gui.screen() == null, 2000, () -> {
            mc.player.setXRot(-28);
            mc.player.setYRot(0);
        });
        for (String sky : List.of("Nebula", "Galaxy", "Aurora", "Sunset", "Void storm")) {
            after(500, () -> set("sky", "sky", sky));
            when(SkyCheck::skyReady, 2500, () -> shot(mc, "sky-" + sky.toLowerCase().replace(' ', '-')));
        }
        after(500, () -> {
            usePicture();
            set("sky", "picture_direction", 0.0);
            set("sky", "picture_height", 30.0);
            set("sky", "picture_size", 45.0);
            mc.player.setYRot(180);
        });
        when(SkyCheck::skyReady, 3000, () -> shot(mc, "sky-picture-on-void-storm"));
        after(500, () -> set("sky", "sky", "Normal"));
        after(2500, () -> shot(mc, "sky-picture-normal-sky"));
        after(500, () -> {
            mc.player.setXRot(10);
            mc.player.setYRot(180);
        });
        after(1500, () -> shot(mc, "sky-normal-looking-away"));
        after(500, () -> {
            ShardClientRuntime.LOGGER.info("[skycheck] done");
            mc.stop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (next >= STEPS.size()) {
                return;
            }
            Step step = STEPS.get(next);
            if (readyAt < 0) {
                if (!step.ready().getAsBoolean()) {
                    return;
                }
                readyAt = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - readyAt >= step.delay()) {
                next++;
                readyAt = -1;
                try {
                    step.action().run();
                } catch (RuntimeException ex) {
                    ShardClientRuntime.LOGGER.error("[skycheck] step {} failed", next, ex);
                }
            }
        });
    }

    private static boolean skyReady() {
        try {
            Module sky = module("sky");
            Field baking = sky.getClass().getDeclaredField("bakingKey");
            Field texture = sky.getClass().getDeclaredField("texture");
            baking.setAccessible(true);
            texture.setAccessible(true);
            return ((String) baking.get(sky)).isEmpty() && texture.get(sky) != null;
        } catch (ReflectiveOperationException ex) {
            return true;
        }
    }

    private static void usePicture() {
        try {
            BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0xB27BFF), 512, 512, new Color(0xFF5FB4)));
            g.fillOval(16, 16, 480, 480);
            g.setColor(new Color(255, 255, 255, 230));
            g.setStroke(new BasicStroke(10));
            g.drawOval(16, 16, 480, 480);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 88));
            g.drawString("SHARD", 110, 290);
            g.dispose();
            Path file = Files.createTempFile("sky-check", ".png");
            ImageIO.write(image, "png", file.toFile());
            Module sky = module("sky");
            Method use = sky.getClass().getDeclaredMethod("usePicture", Path.class);
            use.setAccessible(true);
            use.invoke(sky, file);
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.error("[skycheck] picture failed", ex);
        }
    }

    private static Module module(String id) {
        return ShardClientFull.get().modules().get(id);
    }

    @SuppressWarnings("unchecked")
    private static void set(String moduleId, String settingId, Object value) {
        for (Setting<?> setting : module(moduleId).settings()) {
            if (setting.id().equals(settingId)) {
                ((Setting<Object>) setting).set(value);
                return;
            }
        }
        ShardClientRuntime.LOGGER.warn("[skycheck] no setting {}.{}", moduleId, settingId);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
        ShardClientRuntime.LOGGER.info("[skycheck] {}", name);
    }
}
