package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.HashMap;
import java.util.Map;

public final class Glow {
    private static final ShaderProgram WORLD = new ShaderProgram(Assets.id(Asset.of("shardclient", "core/lyrics_glow")),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, ShaderDefines.EMPTY);
    private static final ShaderProgram WORLD_SEE_THROUGH = new ShaderProgram(
            Assets.id(Asset.of("shardclient", "core/lyrics_glow_see_through")),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, ShaderDefines.EMPTY);

    private static final Map<Asset, RenderType> TYPES = new HashMap<>();
    private static final Map<Asset, RenderType> SEE_THROUGH_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GUI_TYPES = new HashMap<>();

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

    public static void uploadColor(Asset asset, int width, int height, int[] argb) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, argb[y * width + x]);
            }
        }
        Minecraft.getInstance().getTextureManager().register(Assets.id(asset), new Smooth(image));
    }

    public static void release(Asset asset) {
        Minecraft.getInstance().getTextureManager().release(Assets.id(asset));
        GUI_TYPES.remove(Assets.id(asset));
        TYPES.remove(asset);
        SEE_THROUGH_TYPES.remove(asset);
        SKY_TYPES.remove(asset);
    }

    public static void gui(Gfx gfx, Asset texture, int x, int y, int width, int height, int uWidth, int vHeight,
                           int textureWidth, int textureHeight, int argb) {
        gfx.raw().blit(Glow::guiType, Assets.id(texture), x, y, 0, 0, width, height, uWidth, vHeight, textureWidth,
                textureHeight, argb);
    }

    private static RenderType guiType(ResourceLocation texture) {
        return GUI_TYPES.computeIfAbsent(texture, id -> RenderType.create("shardclient_lyrics_glow_gui",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 786432,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_TEX_COLOR))
                        .setTextureState(new RenderStateShard.TextureStateShard(id, TriState.TRUE, false))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false)));
    }

    private static final Map<Asset, RenderType> SKY_TYPES = new HashMap<>();

    static RenderType sky(Asset texture) {
        return SKY_TYPES.computeIfAbsent(texture, id -> RenderType.create("shardclient_sky",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_TEX_COLOR))
                        .setTextureState(new RenderStateShard.TextureStateShard(Assets.id(id), TriState.TRUE, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false)));
    }

    static RenderType world(Asset texture, boolean seeThrough) {
        Map<Asset, RenderType> types = seeThrough ? SEE_THROUGH_TYPES : TYPES;
        return types.computeIfAbsent(texture, id -> RenderType.create(
                seeThrough ? "shardclient_lyrics_glow_see_through" : "shardclient_lyrics_glow",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(seeThrough ? WORLD_SEE_THROUGH : WORLD))
                        .setTextureState(new RenderStateShard.TextureStateShard(Assets.id(id), TriState.TRUE, false))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(seeThrough ? RenderStateShard.NO_DEPTH_TEST : RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false)));
    }

    private static final class Smooth extends DynamicTexture {
        Smooth(NativeImage image) {
            super(image);
            setFilter(true, false);
        }
    }
}
