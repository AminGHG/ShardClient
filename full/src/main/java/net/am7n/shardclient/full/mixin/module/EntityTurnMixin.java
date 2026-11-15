package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void shardclient$freelook(double dx, double dy, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && Features.freelook != null && Features.freelook.active()) {
            Features.freelook.turn(dx, dy);
            ci.cancel();
        }
    }
}
