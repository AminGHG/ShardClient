package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class Glow {
    private static ShaderInstance world;
    private static ShaderInstance worldSeeThrough;

    private static final Map<Asset, RenderType> TYPES = new HashMap<>();
    private static final Map<Asset, RenderType> SEE_THROUGH_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GUI_TYPES = new HashMap<>();

    private Glow() {
    }

    public static void init() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(Assets.id(Asset.of("shardclient", "lyrics_glow")),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, shader -> world = shader);
            context.register(Assets.id(Asset.of("shardclient", "lyrics_glow_see_through")),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, shader -> worldSeeThrough = shader);
        });
    }

    public static void upload(Asset asset, int width, int height, byte[] alpha) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = alpha[y * width + x] & 0xFF;
                image.setPixelRGBA(x, y, a << 24 | 0xFFFFFF);
            }
        }
        Minecraft.getInstance().getTextureManager().register(Assets.id(asset), new Smooth(image));
    }

    public static void release(Asset asset) {
        Minecraft.getInstance().getTextureManager().release(Assets.id(asset));
        GUI_TYPES.remove(Assets.id(asset));
        TYPES.remove(asset);
        SEE_THROUGH_TYPES.remove(asset);
    }

    public static void gui(Gfx gfx, Asset texture, int x, int y, int width, int height, int uWidth, int vHeight,
                           int textureWidth, int textureHeight, int argb) {
        GuiGraphics graphics = gfx.raw();
        VertexConsumer out = graphics.bufferSource().getBuffer(guiType(Assets.id(texture)));
        Matrix4f matrix = graphics.pose().last().pose();
        float u1 = uWidth / (float) textureWidth;
        float v1 = vHeight / (float) textureHeight;
        out.addVertex(matrix, x, y, 0).setUv(0, 0).setColor(argb);
        out.addVertex(matrix, x, y + height, 0).setUv(0, v1).setColor(argb);
        out.addVertex(matrix, x + width, y + height, 0).setUv(u1, v1).setColor(argb);
        out.addVertex(matrix, x + width, y, 0).setUv(u1, 0).setColor(argb);
        graphics.flush();
    }

    private static RenderType guiType(ResourceLocation texture) {
        return GUI_TYPES.computeIfAbsent(texture, id -> RenderType.create("shardclient_lyrics_glow_gui",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 786432, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(id, true, false))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false)));
    }

    static RenderType world(Asset texture, boolean seeThrough) {
        Map<Asset, RenderType> types = seeThrough ? SEE_THROUGH_TYPES : TYPES;
        return types.computeIfAbsent(texture, id -> RenderType.create(
                seeThrough ? "shardclient_lyrics_glow_see_through" : "shardclient_lyrics_glow",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> seeThrough ? worldSeeThrough : world))
                        .setTextureState(new RenderStateShard.TextureStateShard(Assets.id(id), true, false))
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
