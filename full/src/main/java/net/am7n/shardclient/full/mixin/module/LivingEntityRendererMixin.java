package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"),
            cancellable = true)
    private void shardclient$ownNametag(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (!cir.getReturnValueZ() && entity == mc.player && Features.on(Features.ownNametag)
                && !mc.options.getCameraType().isFirstPerson() && !shardclient$serverHidesNametag(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean shardclient$serverHidesNametag(LivingEntity self) {
        if (self.isInvisibleTo(Minecraft.getInstance().player)) {
            return true;
        }
        Team team = self.getTeam();
        if (team != null) {
            Team.Visibility visibility = team.getNameTagVisibility();
            if (visibility == Team.Visibility.NEVER || visibility == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                return true;
            }
        }
        for (Entity passenger : self.getIndirectPassengers()) {
            if (passenger instanceof Display.TextDisplay
                    || (passenger.hasCustomName() && passenger.isCustomNameVisible())) {
                return true;
            }
        }
        return false;
    }
}
