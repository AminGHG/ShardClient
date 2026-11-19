package net.am7n.shardclient.full.platform;

import net.minecraft.client.Minecraft;

public final class Client {
    private Client() {
    }

    public static int fps() {
        return Minecraft.getInstance().getFps();
    }

    public static void sendChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().sendChat(message);
        }
    }
}
