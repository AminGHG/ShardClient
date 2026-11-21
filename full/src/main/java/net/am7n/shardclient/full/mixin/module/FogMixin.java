package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
    private static final float FAR_START = 1.0e6f;
    private static final float FAR_END = 2.0e6f;

    private static boolean shardclient$hide() {
        return Features.sky != null && Features.sky.hidesFog();
    }

    @ModifyVariable(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"),
            ordinal = 0, argsOnly = true)
    private float shardclient$environmentalStart(float value) {
        return shardclient$hide() ? FAR_START : value;
    }

    @ModifyVariable(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"),
            ordinal = 1, argsOnly = true)
    private float shardclient$environmentalEnd(float value) {
        return shardclient$hide() ? FAR_END : value;
    }

    @ModifyVariable(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"),
            ordinal = 2, argsOnly = true)
    private float shardclient$renderDistanceStart(float value) {
        return shardclient$hide() ? FAR_START : value;
    }

    @ModifyVariable(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"),
            ordinal = 3, argsOnly = true)
    private float shardclient$renderDistanceEnd(float value) {
        return shardclient$hide() ? FAR_END : value;
    }
}
