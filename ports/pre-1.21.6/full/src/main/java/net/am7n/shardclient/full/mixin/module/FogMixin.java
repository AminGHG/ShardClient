package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private static void shardclient$hideFog(Camera camera, FogRenderer.FogMode mode, Vector4f color, float renderDistance,
                                            boolean foggy, float partialTick, CallbackInfoReturnable<FogParameters> cir) {
        if (mode == FogRenderer.FogMode.FOG_TERRAIN && Features.sky != null && Features.sky.hidesFog()) {
            FogParameters fog = cir.getReturnValue();
            cir.setReturnValue(new FogParameters(1.0e6f, 2.0e6f, fog.shape(), fog.red(), fog.green(), fog.blue(),
                    fog.alpha()));
        }
    }
}
