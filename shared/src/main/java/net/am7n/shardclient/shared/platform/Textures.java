package net.am7n.shardclient.shared.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

final class Textures {
    private Textures() {
    }

    static void register(Asset asset, int width, int height, int[] argb) {
        NativeImage image = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                image.setPixel(x, y, c);
            }
        }
        Minecraft.getInstance().getTextureManager().register(Assets.id(asset), new DynamicTexture(asset::toString, image));
    }
}
