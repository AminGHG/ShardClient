package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudsMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void shardclient$hideClouds(CallbackInfo ci) {
        if (Features.sky != null && Features.sky.hidesClouds()) {
            ci.cancel();
        }
    }
}
