package net.am7n.shardclient.shared.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Game {
    private Game() {
    }

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }
}
