package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

/** Development only: screenshots the Full Client's title screen and server list, then quits. */
final class MenuCapture {

    private MenuCapture() {
    }

    static void register() {
        long[] start = {0};
        int[] step = {0};
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (step[0] == 0 && mc.gui.overlay() == null && mc.gui.screen() instanceof ShardTitleScreen) {
                start[0] = System.currentTimeMillis();
                step[0] = 1;
            }
            if (step[0] == 0) {
                return;
            }
            long t = System.currentTimeMillis() - start[0];
            if (step[0] == 1 && t > 1200) {
                shot(mc, "menu-title");
                step[0] = 2;
            } else if (step[0] == 2 && t > 1400) {
                mc.gui.setScreen(new JoinMultiplayerScreen(mc.gui.screen()));
                step[0] = 3;
            } else if (step[0] == 3 && t > 4200) {
                shot(mc, "menu-servers");
                step[0] = 4;
            } else if (step[0] == 4 && t > 4800) {
                mc.stop();
                step[0] = 5;
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
}
