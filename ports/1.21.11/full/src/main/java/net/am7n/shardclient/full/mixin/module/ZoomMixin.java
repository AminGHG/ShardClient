package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void shardclient$zoom(Camera camera, float partialTick, boolean world, CallbackInfoReturnable<Float> cir) {
        if (Features.zoom != null && Features.zoom.isEnabled() && (world || Features.zoom.affectsHand())) {
            float factor = Features.zoom.factor(partialTick);
            if (factor != 1f) {
                cir.setReturnValue(cir.getReturnValueF() / factor);
            }
        }
    }
}
