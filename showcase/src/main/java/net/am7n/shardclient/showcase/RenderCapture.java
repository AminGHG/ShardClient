package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.shared.render.ShardPvpLook;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class RenderCapture {
    private record Step(long atMs, Runnable action) {
    }

    private RenderCapture() {
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        List<Step> steps = new ArrayList<>();
        long[] t = {1500};
        java.util.function.BiConsumer<Runnable, String> shot = (setup, name) -> {
            steps.add(new Step(t[0], setup));
            steps.add(new Step(t[0] + 1800, () -> capture(mc, name)));
            t[0] += 2200;
        };

        steps.add(new Step(300, () -> {
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }));
        steps.add(new Step(500, () -> {
            Set<String> on = Set.of("fullbright", "zoom", "freelook", "fps", "keystrokes", "cps", "armor",
                    "crosshair", "toggle_sprint");
            for (Module module : ShardClientFull.get().modules().all()) {
                module.setEnabled(on.contains(module.id()));
            }
        }));
        shot.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new ShardTitleScreen());
            ShardClientFull.get().openMenu(mc);
        }, "render-menu-visual");
        shot.accept(() -> clickTab(mc, 1), "render-menu-hud");
        shot.accept(() -> clickTab(mc, 2), "render-menu-pvp");
        shot.accept(() -> clickTab(mc, 4), "render-menu-settings");
        shot.accept(() -> {
            Theme.use(ShardPvpLook.LIGHT);
            mc.gui.setScreen(new ShardTitleScreen());
            ShardClientFull.get().openMenu(mc);
        }, "render-menu-light");
        shot.accept(() -> {
            Element root = root(mc.gui.screen());
            if (root != null) {
                click(mc, root.x() + root.width() - 70, root.y() + 18);
                mc.gui.screen().charTyped(new CharacterEvent('c'));
                mc.gui.screen().charTyped(new CharacterEvent('o'));
            }
        }, "render-menu-search-light");
        shot.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new ShardTitleScreen());
        }, "render-title");
        shot.accept(() -> mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen())), "render-servers");
        steps.add(new Step(t[0], mc::stop));

        long[] start = {0};
        int[] next = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (start[0] == 0) {
                if (client.gui.overlay() == null && client.gui.screen() instanceof ShardTitleScreen) {
                    start[0] = System.currentTimeMillis();
                }
                return;
            }
            long elapsed = System.currentTimeMillis() - start[0];
            while (next[0] < steps.size() && elapsed >= steps.get(next[0]).atMs()) {
                steps.get(next[0]++).action().run();
            }
        });
    }

    private static void clickTab(Minecraft mc, int index) {
        Element root = root(mc.gui.screen());
        if (root != null) {
            click(mc, root.x() + 30, root.y() + 10 + 30 + index * 22 + 10);
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
        Element root = root(mc.gui.screen());
        double scale = mc.getWindow().getGuiScale();
        String rect = root == null || root.width() >= mc.gui.screen().width - 2
                ? "full"
                : Math.round(root.x() * scale) + " " + Math.round(root.y() * scale) + " "
                + Math.round(root.width() * scale) + " " + Math.round(root.height() * scale) + " " + scale;
        try {
            Path dir = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".rect.txt"), rect);
        } catch (IOException ignored) {
        }
    }
}
