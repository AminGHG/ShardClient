package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
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
