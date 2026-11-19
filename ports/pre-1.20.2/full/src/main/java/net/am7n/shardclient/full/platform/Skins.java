package net.am7n.shardclient.full.platform;

import net.minecraft.client.player.AbstractClientPlayer;

final class Skins {
    private Skins() {
    }

    static boolean slim(AbstractClientPlayer player) {
        return "slim".equals(player.getModelName());
    }
}
