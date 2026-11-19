package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.platform.HudHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void shardclient$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (HudHooks.drawCrosshair(graphics)) {
            ci.cancel();
        }
    }
}
