package net.am7n.shardclient.full.platform;

import net.minecraft.client.Minecraft;

public final class Client {
    private Client() {
    }

    public static int fps() {
        return Minecraft.getInstance().getFps();
    }

    public static float daylight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 1f;
        }
        double angle = (mc.level.getOverworldClockTime() % 24000L - 6000L) / 24000.0 * Math.PI * 2;
        double day = Math.max(0, Math.min(1, Math.cos(angle) * 2 + 0.5));
        return (float) (0.22 + 0.78 * day);
    }

    public static void sendChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().sendChat(message);
        }
    }
}
