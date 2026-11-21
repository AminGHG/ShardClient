package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void shardclient$hideFog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean foggy,
                                            float partialTick, CallbackInfo ci) {
        if (mode == FogRenderer.FogMode.FOG_TERRAIN && Features.sky != null && Features.sky.hidesFog()) {
            RenderSystem.setShaderFogStart(1.0e6f);
            RenderSystem.setShaderFogEnd(2.0e6f);
        }
    }
}
