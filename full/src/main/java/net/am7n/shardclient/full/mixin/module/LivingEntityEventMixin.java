package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.pvp.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEventMixin {
    private static final byte TOTEM_EVENT = 35;

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void shardclient$totem(byte event, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (event == TOTEM_EVENT && self.level().isClientSide()) {
            CombatTracker.totemPopped(self);
        }
    }
}
