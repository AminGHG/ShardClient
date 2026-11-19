package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import com.mojang.blaze3d.platform.CompareOp;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Optional;
import java.util.function.Consumer;

final class Backend {
    static final RenderPipeline.Snippet WORLD_TEXT = RenderPipelines.TEXT_SNIPPET;

    private Backend() {
    }

    static void onRender(Consumer<World> handler) {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> handler.accept(
                new World(context.submitNodeCollector(), context.poseStack(), context.gameRenderer())));
    }

    static Camera camera(GameRenderer renderer) {
        return renderer.getMainCamera();
    }

    static RenderPipeline.Builder glowShaders(RenderPipeline.Builder builder, boolean seeThrough) {
        builder = builder.withVertexShader("core/text")
                .withFragmentShader(Assets.id(Asset.of("shardclient", "core/lyrics_glow")));
        return seeThrough ? builder.withShaderDefine("IS_SEE_THROUGH") : builder;
    }

    static RenderPipeline.Builder additive(RenderPipeline.Builder builder) {
        return builder.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING));
    }

    static RenderPipeline.Builder depthTestNoWrite(RenderPipeline.Builder builder) {
        return builder.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false));
    }

    static RenderPipeline.Builder noDepth(RenderPipeline.Builder builder) {
        return builder.withDepthStencilState(Optional.empty());
    }
}
