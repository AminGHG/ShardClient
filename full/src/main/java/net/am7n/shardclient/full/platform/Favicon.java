package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.FaviconTexture;

import java.io.IOException;

public final class Favicon implements AutoCloseable {
    private final FaviconTexture texture;

    private Favicon(FaviconTexture texture) {
        this.texture = texture;
    }

    public static Favicon forServer(String address) {
        return new Favicon(FaviconTexture.forServer(Minecraft.getInstance().getTextureManager(), address));
    }

    public void upload(byte[] png) throws IOException {
        texture.upload(NativeImage.read(png));
    }

    public Asset asset() {
        return Assets.asset(texture.textureLocation());
    }

    @Override
    public void close() {
        texture.close();
    }
}
