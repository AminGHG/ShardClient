package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void shardclient$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientLevel && Features.on(Features.weather)) {
            cir.setReturnValue(Features.weather.rain());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void shardclient$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientLevel && Features.on(Features.weather)) {
            cir.setReturnValue(Features.weather.thunder());
        }
    }
}
