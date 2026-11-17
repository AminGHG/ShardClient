package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla creates post-effect uniform buffers as read-only. ShardClient's own passes get
 * {@link GpuBuffer#USAGE_COPY_DST} as well, so values such as the motion blur amount can be rewritten at runtime.
 */
@Mixin(PostPass.class)
public abstract class PostPassMixin {
    @Shadow
    @Final
    private RenderPipeline pipeline;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createBuffer(Ljava/util/function/Supplier;ILjava/nio/ByteBuffer;)Lcom/mojang/blaze3d/buffers/GpuBuffer;"),
            index = 1)
    private int shardclient$writableUniforms(int usage) {
        if ("shardclient".equals(pipeline.getLocation().getNamespace())) {
            return usage | GpuBuffer.USAGE_COPY_DST;
        }
        return usage;
    }
}
