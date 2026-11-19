package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public final class Favicon implements AutoCloseable {
    private final Asset asset;
    private DynamicTexture texture;

    private Favicon(Asset asset) {
        this.asset = asset;
    }

    public static Favicon forServer(String address) {
        String key = Integer.toHexString(address.toLowerCase(Locale.ROOT).hashCode());
        return new Favicon(Asset.of("shardclient", "servers/" + key + "/icon"));
    }

    public void upload(byte[] png) throws IOException {
        NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
        if (texture == null) {
            texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(Assets.id(asset), texture);
        } else {
            texture.setPixels(image);
            texture.upload();
        }
    }

    public Asset asset() {
        return asset;
    }

    @Override
    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(Assets.id(asset));
            texture = null;
        }
    }
}
