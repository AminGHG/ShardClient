package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.media.Lyrics;
import net.am7n.shardclient.full.media.MediaWatcher;
import net.am7n.shardclient.full.media.NowPlaying;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class FeatureCheck {
    private record Step(long delay, BooleanSupplier ready, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static long readyAt = -1;
    private static boolean spinning;
    private static boolean sampling;
    private static long fpsSum;
    private static int fpsCount;

    private FeatureCheck() {
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
            mc.options.guiScale().set(3);
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            mc.resizeGui();
            module("motion_blur").setEnabled(false);
            for (String id : List.of("lyrics", "waypoints", "hit_color", "screenshot_copy")) {
                module(id).setEnabled(true);
            }
            set("lyrics", "place", "In front");
            set("lyrics", "layout", "Words");
            set("lyrics", "font", "Caveat");
            set("lyrics", "color", "Gradient");
            set("lyrics", "glow_v2", "Strong");
            set("lyrics", "glow_color", "Lilac");
            set("lyrics", "enter", "Rise");
            set("lyrics", "through_walls_v2", true);
            long now = System.currentTimeMillis();
            MediaWatcher.get().useForPreview(new NowPlaying("Spotify", "Check", "Check", "", 120000, 0, now, true),
                    new Lyrics(List.of(new Lyrics.Line(0, "Lights are falling from the sky tonight"),
                            new Lyrics.Line(6000, "We keep running through the endless night"),
                            new Lyrics.Line(12000, "Sing it louder"),
                            new Lyrics.Line(18000, "Every single word is shining bright"),
                            new Lyrics.Line(24000, "Hold on"),
                            new Lyrics.Line(30000, "Lights are falling from the sky tonight"),
                            new Lyrics.Line(36000, "We keep running through the endless night"),
                            new Lyrics.Line(42000, "Every single word is shining bright"),
                            new Lyrics.Line(48000, "Sing it louder"),
                            new Lyrics.Line(54000, "Hold on to the light"))));
            ShardClientRuntime.LOGGER.info("[check] motion blur chain: {}", mc.getShaderManager().getPostChain(
                    Identifier.fromNamespaceAndPath("shardclient", "motion_blur_medium"), LevelTargetBundle.MAIN_TARGETS));
        });
        after(400, () -> ShardClientFull.get().openMenu(mc));
        after(600, () -> {
            Element root = root(mc.gui.screen());
            if (root != null) {
                click(mc, root.x() + root.width() - 70, root.y() + 18);
                for (char c : "lyr".toCharArray()) {
                    mc.gui.screen().charTyped(new CharacterEvent(c));
                }
            }
        });
        after(800, () -> {
            Element root = root(mc.gui.screen());
            if (root != null) {
                click(mc, root.x() + 200, root.y() + 52);
            }
        });
        after(1500, () -> capture(mc, "check-lyrics-1"));
        after(300, () -> scroll(mc, -4));
        after(700, () -> capture(mc, "check-lyrics-2"));
        after(300, () -> scroll(mc, -4));
        after(700, () -> capture(mc, "check-lyrics-3"));
        after(300, () -> scroll(mc, -6));
        after(700, () -> capture(mc, "check-lyrics-4"));
        after(300, () -> {
            set("lyrics", "place", "Screen");
            scroll(mc, 30);
        });
        after(1000, () -> capture(mc, "check-lyrics-screen-settings"));
        after(300, () -> {
            set("lyrics", "place", "In front");
            mc.gui.setScreen(null);
            mc.createWorldOpenFlows().openWorld("Demo_World", () -> mc.gui.setScreen(new ShardTitleScreen()));
        });
        when(() -> mc.level != null && mc.player != null && mc.gui.screen() == null, 500, () -> {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true));
            }
            mc.player.setXRot(-8);
        });
        after(5500, () -> {
            int x = mc.player.getBlockX();
            int y = mc.player.getBlockY();
            int z = mc.player.getBlockZ();
            float yaw = (float) Math.toRadians(mc.player.getYRot());
            int fx = (int) Math.round(-Math.sin(yaw) * 14);
            int fz = (int) Math.round(Math.cos(yaw) * 14);
            addWaypoint("Base", x + fx + fz / 2, y, z + fz - fx / 2);
            addWaypoint("Mine", x + fx * 4 - fz * 2, y + 2, z + fz * 4 + fx * 2);
            addWaypoint("Far away", x + fx * 30 + fz * 10, y, z + fz * 30 - fx * 10);
        });
        after(4000, () -> capture(mc, "check-world-words-flat"));
        after(300, () -> {
            set("lyrics", "font", "Minecraft");
            set("lyrics", "letters", "3D");
            set("lyrics", "color", "Gold");
            set("lyrics", "glow_v2", "Soft");
            set("lyrics", "glow_color", "Same as text");
        });
        after(5000, () -> capture(mc, "check-world-words-3d"));
        after(300, () -> {
            set("lyrics", "layout", "Lines");
            set("lyrics", "font", "Righteous");
            set("lyrics", "letters", "Flat");
            set("lyrics", "color", "Gradient");
            set("lyrics", "glow_v2", "Strong");
        });
        after(5000, () -> capture(mc, "check-world-lines-neon"));
        after(300, () -> {
            set("lyrics", "preset", "Custom");
            set("lyrics", "place", "In front");
            set("lyrics", "layout", "Words");
            set("lyrics", "font", "Righteous");
        });
        for (int round = 0; round < 3; round++) {
            for (String glow : List.of("Strong", "Off")) {
                after(200, () -> {
                    set("lyrics", "glow_v2", glow);
                    fpsSum = 0;
                    fpsCount = 0;
                    sampling = true;
                });
                after(3000, () -> {
                    sampling = false;
                    ShardClientRuntime.LOGGER.info("[check] fps with glow {}: {}", glow, fpsSum / Math.max(1, fpsCount));
                });
            }
        }
        after(300, () -> set("lyrics", "preset", "Starry Sky"));
        after(6000, () -> capture(mc, "check-world-sky"));
        after(300, () -> {
            set("lyrics", "preset", "Custom");
            set("lyrics", "place", "Screen");
            set("lyrics", "layout", "Words");
            set("lyrics", "glow_v2", "Strong");
        });
        after(5000, () -> capture(mc, "check-world-screen"));
        after(300, () -> set("lyrics", "glow_v2", "Off"));
        after(4000, () -> capture(mc, "check-world-screen-noglow"));
        after(300, () -> set("lyrics", "glow_v2", "Strong"));
        after(300, () -> set("lyrics", "glow_v2", "Off"));
        after(4000, () -> capture(mc, "check-world-screen-noglow-2"));
        after(300, () -> {
            module("motion_blur").setEnabled(true);
            spinning = true;
        });
        after(500, () -> capture(mc, "check-world-blur"));
        after(300, () -> spinning = false);
        after(1500, mc::stop);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (sampling) {
                fpsSum += client.getFps();
                fpsCount++;
            }
            if (spinning && client.player != null) {
                client.player.setYRot(client.player.getYRot() + 12);
            }
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
                    ShardClientRuntime.LOGGER.error("[check] step {} failed", next, ex);
                }
            }
        });
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
        ShardClientRuntime.LOGGER.warn("[check] no setting {}.{}", moduleId, settingId);
    }

    private static void addWaypoint(String name, int x, int y, int z) {
        try {
            Method add = module("waypoints").getClass().getDeclaredMethod("add", String.class, int.class, int.class,
                    int.class);
            add.setAccessible(true);
            add.invoke(module("waypoints"), name, x, y, z);
        } catch (ReflectiveOperationException ex) {
            ShardClientRuntime.LOGGER.error("[check] add waypoint failed", ex);
        }
    }

    private static void scroll(Minecraft mc, double amount) {
        Element root = root(mc.gui.screen());
        if (root != null) {
            mc.gui.screen().mouseScrolled(root.x() + 250, root.y() + 150, 0, amount);
        }
    }

    private static void click(Minecraft mc, double x, double y) {
        Screen screen = mc.gui.screen();
        screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        screen.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
    }

    private static Element root(Screen screen) {
        if (!(screen instanceof ShardScreen)) {
            return null;
        }
        try {
            Field field = ShardScreen.class.getDeclaredField("root");
            field.setAccessible(true);
            return (Element) field.get(screen);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
        ShardClientRuntime.LOGGER.info("[check] captured {} at {} fps", name, mc.getFps());
    }
}
