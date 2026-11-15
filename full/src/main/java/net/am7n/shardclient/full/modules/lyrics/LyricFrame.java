package net.am7n.shardclient.full.modules.lyrics;

import net.minecraft.network.chat.Component;

public record LyricFrame(Component text, int color, float alpha, float offsetY, float scale, Role role, int lineId) {
    public enum Role { CURRENT, PREVIOUS, NEXT }
}
