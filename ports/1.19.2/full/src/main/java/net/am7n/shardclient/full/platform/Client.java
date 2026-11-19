package net.am7n.shardclient.full.platform;

import net.minecraft.client.Minecraft;

public final class Client {
    private Client() {
    }

    public static int fps() {
        String text = Minecraft.getInstance().fpsString;
        try {
            return Integer.parseInt(text.substring(0, text.indexOf(' ')));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public static void sendChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.chatSigned(message, null);
        }
    }
}
