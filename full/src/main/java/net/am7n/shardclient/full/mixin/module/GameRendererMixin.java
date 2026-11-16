package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    @Unique
    private boolean shardclient$blurFailed;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void shardclient$noHurtCam(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
        if (Features.on(Features.noHurtCam)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void shardclient$motionBlur(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        if (!renderLevel || minecraft.level == null || !Features.on(Features.motionBlur) || shardclient$blurFailed) {
            return;
        }
        try {
            PostChain chain = minecraft.getShaderManager().getPostChain(Features.motionBlur.effect(),
                    LevelTargetBundle.MAIN_TARGETS);
            if (chain != null) {
                chain.process(mainRenderTarget, resourcePool);
            }
        } catch (RuntimeException ex) {
            shardclient$blurFailed = true;
            ShardClientRuntime.LOGGER.error("Motion blur failed and was turned off", ex);
        }
    }
}
