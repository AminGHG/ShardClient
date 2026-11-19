package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.HashMap;
import java.util.Map;

public final class Glow {
    private static final RenderPipeline WORLD = RenderPipelines.register(Backend.depthTestNoWrite(Backend.additive(
            Backend.glowShaders(RenderPipeline.builder(Backend.WORLD_TEXT)
                    .withLocation(Assets.id(Asset.of("shardclient", "pipeline/lyrics_glow"))), false)))
            .build());

    private static final RenderPipeline WORLD_SEE_THROUGH = RenderPipelines.register(Backend.noDepth(Backend.additive(
            Backend.glowShaders(RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
                    .withLocation(Assets.id(Asset.of("shardclient", "pipeline/lyrics_glow_see_through"))), true)))
            .build());

    private static final RenderPipeline GUI = RenderPipelines.register(Backend.additive(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Assets.id(Asset.of("shardclient", "pipeline/lyrics_glow_gui"))))
            .build());

    private static final Map<Asset, RenderType> TYPES = new HashMap<>();
    private static final Map<Asset, RenderType> SEE_THROUGH_TYPES = new HashMap<>();

    private Glow() {
    }

    public static void init() {
    }

    public static void upload(Asset asset, int width, int height, byte[] alpha) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = alpha[y * width + x] & 0xFF;
                image.setPixel(x, y, a << 24 | 0xFFFFFF);
            }
        }
        Minecraft.getInstance().getTextureManager().register(Assets.id(asset), new Smooth(image));
    }

    public static void release(Asset asset) {
        Minecraft.getInstance().getTextureManager().release(Assets.id(asset));
        TYPES.remove(asset);
        SEE_THROUGH_TYPES.remove(asset);
    }

    public static void gui(Gfx gfx, Asset texture, int x, int y, int width, int height, int uWidth, int vHeight,
                           int textureWidth, int textureHeight, int argb) {
        gfx.raw().blit(GUI, Assets.id(texture), x, y, 0, 0, width, height, uWidth, vHeight, textureWidth,
                textureHeight, argb);
    }

    static RenderType world(Asset texture, boolean seeThrough) {
        Map<Asset, RenderType> types = seeThrough ? SEE_THROUGH_TYPES : TYPES;
        return types.computeIfAbsent(texture, id -> RenderType.create(
                seeThrough ? "shardclient_lyrics_glow_see_through" : "shardclient_lyrics_glow",
                RenderSetup.builder(seeThrough ? WORLD_SEE_THROUGH : WORLD).withTexture("Sampler0", Assets.id(id))
                        .useLightmap().createRenderSetup()));
    }

    private static final class Smooth extends DynamicTexture {
        Smooth(NativeImage image) {
            super(() -> "ShardClient lyrics glow", image);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
