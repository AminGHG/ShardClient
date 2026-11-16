package net.am7n.shardclient.full.modules.lyrics;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GlowPipelines {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("shardclient", "core/lyrics_glow");

    static final RenderPipeline WORLD = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("shardclient", "pipeline/lyrics_glow"))
            .withVertexShader("core/text")
            .withFragmentShader(SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build());

    static final RenderPipeline WORLD_SEE_THROUGH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("shardclient", "pipeline/lyrics_glow_see_through"))
                    .withVertexShader("core/text")
                    .withFragmentShader(SHADER)
                    .withShaderDefine("IS_SEE_THROUGH")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withDepthStencilState(Optional.empty())
                    .build());

    static final RenderPipeline GUI = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("shardclient", "pipeline/lyrics_glow_gui"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .build());

    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();
    private static final Map<Identifier, RenderType> SEE_THROUGH_TYPES = new HashMap<>();

    private GlowPipelines() {
    }

    public static void init() {
    }

    static RenderType world(Identifier texture, boolean seeThrough) {
        Map<Identifier, RenderType> types = seeThrough ? SEE_THROUGH_TYPES : TYPES;
        return types.computeIfAbsent(texture, id -> RenderType.create(
                seeThrough ? "shardclient_lyrics_glow_see_through" : "shardclient_lyrics_glow",
                RenderSetup.builder(seeThrough ? WORLD_SEE_THROUGH : WORLD).withTexture("Sampler0", id).useLightmap()
                        .createRenderSetup()));
    }

    static void forget(Identifier texture) {
        TYPES.remove(texture);
        SEE_THROUGH_TYPES.remove(texture);
    }
}
