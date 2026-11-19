package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.Consumer;

final class Backend {
    static final RenderPipeline.Snippet WORLD_TEXT = RenderPipelines.TEXT_SNIPPET;

    private Backend() {
    }

    static void onRender(Consumer<World> handler) {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> handler.accept(
                new World(context.commandQueue(), context.matrices(), context.gameRenderer())));
    }

    static Camera camera(GameRenderer renderer) {
        return renderer.getMainCamera();
    }

    static RenderPipeline.Builder glowShaders(RenderPipeline.Builder builder, boolean seeThrough) {
        return builder.withVertexShader(seeThrough ? "core/rendertype_text_see_through" : "core/rendertype_text")
                .withFragmentShader(Assets.id(Asset.of("shardclient",
                        seeThrough ? "core/lyrics_glow_see_through" : "core/lyrics_glow")));
    }

    static RenderPipeline.Builder additive(RenderPipeline.Builder builder) {
        return builder.withBlend(BlendFunction.LIGHTNING);
    }

    static RenderPipeline.Builder depthTestNoWrite(RenderPipeline.Builder builder) {
        return builder.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false);
    }

    static RenderPipeline.Builder noDepth(RenderPipeline.Builder builder) {
        return builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false);
    }
}
