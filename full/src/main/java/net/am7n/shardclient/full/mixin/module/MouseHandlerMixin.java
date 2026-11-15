package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void shardclient$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Features.zoom != null && Features.zoom.scroll(vertical)) {
            ci.cancel();
        }
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", ordinal = 0))
    private double shardclient$zoomSensitivity(Double sensitivity) {
        double value = sensitivity;
        return Features.zoom != null ? Features.zoom.sensitivity(value) : value;
    }
}
