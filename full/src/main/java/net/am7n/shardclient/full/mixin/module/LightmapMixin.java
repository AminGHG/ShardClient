package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void shardclient$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (Features.fullbright != null && Features.fullbright.isEnabled()) {
            state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, Features.fullbright.strength());
        }
    }
}
