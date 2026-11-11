package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.shared.render.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only: screenshots the Full Client's title screen in each main-button colour, with the look's font and
 * with Minecraft's font on the buttons, then the server list, then quits.
 */
final class MenuCapture {

    private record Step(long atMs, Runnable action) {
    }

    private MenuCapture() {
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        List<Step> steps = new ArrayList<>();
        long t = 1200;
        for (Theme.PrimaryStyle style : List.of(Theme.PrimaryStyle.WHITE, Theme.PrimaryStyle.OUTLINE, Theme.PrimaryStyle.SOFT)) {
            for (boolean minecraftFont : new boolean[]{false, true}) {
                steps.add(new Step(t, () -> {
                    Theme.usePrimaryStyle(style);
                    Theme.useMinecraftButtonFont(minecraftFont);
                }));
                String name = "menu-" + style.name().toLowerCase() + (minecraftFont ? "-mcfont" : "-inter");
                steps.add(new Step(t + 250, () -> shot(mc, name)));
                t += 400;
            }
        }
        steps.add(new Step(t, () -> mc.gui.setScreen(new JoinMultiplayerScreen(mc.gui.screen()))));
        steps.add(new Step(t + 2500, () -> shot(mc, "menu-servers-mcfont")));
        steps.add(new Step(t + 2600, () -> Theme.useMinecraftButtonFont(false)));
        steps.add(new Step(t + 2900, () -> shot(mc, "menu-servers-inter")));
        steps.add(new Step(t + 3400, mc::stop));

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
