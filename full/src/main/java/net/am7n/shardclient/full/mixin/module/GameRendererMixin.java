package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void shardclient$noHurtCam(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
        if (Features.on(Features.noHurtCam)) {
            ci.cancel();
        }
    }
}
