package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.platform.Chat;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.modules.Keys;
import net.minecraft.network.chat.Component;

public final class CopyCoordinates extends Module {
    public CopyCoordinates() {
        super("copy_coordinates", "Copy coordinates",
                "Copies your position when you press its key (set it in Controls → ShardClient).", Category.UTILITY);
        setEnabled(true);
    }

    @Override
    public void tick() {
        while (Keys.copyCoordinates != null && Keys.copyCoordinates.consumeClick()) {
            if (mc.player == null) {
                continue;
            }
            String text = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
            mc.keyboardHandler.setClipboard(text);
            Chat.actionBar(Component.literal("Copied " + text));
        }
    }
}
