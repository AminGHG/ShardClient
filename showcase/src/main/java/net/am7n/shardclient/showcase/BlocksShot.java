package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.full.platform.Glow;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Asset;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

final class BlocksShot {
    private record Step(long delay, BooleanSupplier ready, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static long readyAt = -1;

    private BlocksShot() {
    }

    private static void after(long delay, Runnable action) {
        STEPS.add(new Step(delay, () -> true, action));
    }

    private static void when(BooleanSupplier ready, long delay, Runnable action) {
        STEPS.add(new Step(delay, ready, action));
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        float pitch = Float.parseFloat(System.getProperty("shardclient.showcase.pitch", "-15"));
        float yaw = Float.parseFloat(System.getProperty("shardclient.showcase.yaw", "0"));
        when(() -> mc.gui.overlay() == null && mc.gui.screen() instanceof ShardTitleScreen, 300, () -> {
            mc.options.framerateLimit().set(60);
            mc.options.renderDistance().set(20);
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            module("motion_blur").setEnabled(false);
            set("sky", "sky", "Nebula");
            set("sky", "day_night", false);
            set("sky", "brightness", 100.0);
            set("sky", "hide_clouds", true);
            set("sky", "picture", false);
            module("sky").setEnabled(true);
            mc.createWorldOpenFlows().openWorld("Demo_World", () -> mc.gui.setScreen(new ShardTitleScreen()));
        });
        when(() -> mc.level != null && mc.player != null && mc.gui.screen() == null, 1500, () -> {
            command(mc, "time set noon");
            command(mc, "weather clear");
            command(mc, "tick freeze");
            command(mc, "gamemode spectator @a");
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        when(BlocksShot::skyReady, 3000, () -> { });
        String[] spots = System.getProperty("shardclient.showcase.spots", "0,0,2").split(";");
        int index = 0;
        for (String spot : spots) {
            String[] p = spot.split(",");
            String name = "blocks-" + index++;
            float yawHere = Float.parseFloat(p[0]);
            String move = p[1];
            String lift = p[2];
            after(200, () -> {
                command(mc, "execute as @a at @s positioned ~" + move + " ~ ~ positioned over motion_blocking run tp @s ~ ~" + lift + " ~ " + yawHere + " " + pitch);
            });
            after(5000, () -> {
                mc.player.setXRot(pitch);
                mc.player.setYRot(yawHere);
                solid(0xFF000000, "black");
            });
            after(1200, () -> shot(mc, name + "-black"));
            after(200, () -> solid(0xFFFFFFFF, "white"));
            after(1200, () -> shot(mc, name + "-white"));
        }
        after(500, () -> {
            ShardClientRuntime.LOGGER.info("[blocksshot] done");
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
                    ShardClientRuntime.LOGGER.error("[blocksshot] step {} failed", next, ex);
                }
            }
        });
    }

    private static void command(Minecraft mc, String command) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            ShardClientRuntime.LOGGER.warn("[blocksshot] no server for {}", command);
            return;
        }
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void solid(int argb, String name) {
        try {
            int[] pixels = new int[16 * 16];
            Arrays.fill(pixels, argb);
            Asset asset = Asset.of("shardclient", "sky/solid_" + name);
            Glow.uploadColor(asset, 16, 16, pixels);
            Field texture = module("sky").getClass().getDeclaredField("texture");
            texture.setAccessible(true);
            texture.set(module("sky"), asset);
        } catch (ReflectiveOperationException ex) {
            ShardClientRuntime.LOGGER.error("[blocksshot] solid sky failed", ex);
        }
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
        ShardClientRuntime.LOGGER.warn("[blocksshot] no setting {}.{}", moduleId, settingId);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
        ShardClientRuntime.LOGGER.info("[blocksshot] {}", name);
    }
}
