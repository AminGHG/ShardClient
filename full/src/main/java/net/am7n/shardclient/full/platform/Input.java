package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

public final class Input {
    private Input() {
    }

    public static boolean isDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }
}
