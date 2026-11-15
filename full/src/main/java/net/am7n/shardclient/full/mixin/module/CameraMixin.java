package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void shardclient$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Features.zoom != null && Features.zoom.isEnabled()) {
            float factor = Features.zoom.factor(partialTick);
            if (factor != 1f) {
                cir.setReturnValue(cir.getReturnValueF() / factor);
            }
        }
    }

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void shardclient$zoomHand(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Features.zoom != null && Features.zoom.isEnabled() && Features.zoom.affectsHand()) {
            float factor = Features.zoom.factor(partialTick);
            if (factor != 1f) {
                cir.setReturnValue(cir.getReturnValueF() / factor);
            }
        }
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float shardclient$freelookYaw(Entity entity, float partialTick) {
        if (Features.freelook != null && Features.freelook.active() && entity == Minecraft.getInstance().player) {
            return Features.freelook.yaw();
        }
        return entity.getViewYRot(partialTick);
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float shardclient$freelookPitch(Entity entity, float partialTick) {
        if (Features.freelook != null && Features.freelook.active() && entity == Minecraft.getInstance().player) {
            return Features.freelook.pitch();
        }
        return entity.getViewXRot(partialTick);
    }
}
