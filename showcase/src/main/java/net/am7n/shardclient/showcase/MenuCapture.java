package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.menu.MenuScreens;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.shared.render.SemiVanillaLook;
import net.am7n.shardclient.shared.render.ShardPvpLook;
import net.am7n.shardclient.shared.render.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only: screenshots the Full Client's menus (title, server list, world list, client menu) in the
 * website look and in Semi Vanilla, Minecraft's title screen with the style switch, and the player badge; then
 * quits.
 */
final class MenuCapture {

    private record Step(long atMs, Runnable action) {
    }

    private MenuCapture() {
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        List<Step> steps = new ArrayList<>();
        long[] t = {1200};
        java.util.function.BiConsumer<Runnable, String> at = (action, shot) -> {
            steps.add(new Step(t[0], action));
            steps.add(new Step(t[0] + 1600, () -> shot(mc, shot)));
            t[0] += 1900;
        };
        at.accept(() -> Theme.use(ShardPvpLook.DARK), "v-title");
        at.accept(() -> Theme.use(SemiVanillaLook.INSTANCE), "v-title-semi");
        at.accept(() -> mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen())), "v-servers-semi");
        at.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen()));
        }, "v-servers");
        at.accept(() -> mc.gui.setScreen(new SelectWorldScreen(new ShardTitleScreen())), "v-worlds");
        at.accept(() -> {
            Theme.use(SemiVanillaLook.INSTANCE);
            mc.gui.setScreen(new SelectWorldScreen(new ShardTitleScreen()));
        }, "v-worlds-semi");
        at.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new ShardTitleScreen());
            ShardClientFull.get().openMenu(mc);
        }, "v-clientmenu");
        at.accept(() -> mc.gui.setScreen(new BadgePreviewScreen()), "v-badge");
        at.accept(MenuScreens::toggle, "v-vanilla-title");
        steps.add(new Step(t[0], MenuScreens::toggle));
        steps.add(new Step(t[0] + 600, mc::stop));

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

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
}
