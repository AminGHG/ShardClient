package net.am7n.shardclient.shared.platform;

import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Game {
    private Game() {
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }

    public static void joinServer(java.util.UUID profileId, String accessToken, String serverId)
            throws AuthenticationException {
        Minecraft.getInstance().services().sessionService().joinServer(profileId, accessToken, serverId);
    }

    public static boolean loaded() {
        return Minecraft.getInstance().isGameLoadFinished();
    }
}
