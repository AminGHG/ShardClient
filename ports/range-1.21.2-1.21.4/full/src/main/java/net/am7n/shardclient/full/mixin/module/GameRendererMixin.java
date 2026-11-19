package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.full.modules.visual.MotionBlur;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
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
    private CrossFrameResourcePool resourcePool;

    @Unique
    private boolean shardclient$blurFailed;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void shardclient$motionBlur(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        if (!renderLevel || minecraft.level == null || !Features.on(Features.motionBlur) || shardclient$blurFailed) {
            return;
        }
        float blend = Features.motionBlur.blendFactor();
        if (blend <= 0) {
            return;
        }
        try {
            PostChain chain = minecraft.getShaderManager().getPostChain(Assets.id(MotionBlur.EFFECT),
                    LevelTargetBundle.MAIN_TARGETS);
            if (chain == null) {
                return;
            }
            chain.setUniform("BlendFactor", blend);
            chain.process(minecraft.getMainRenderTarget(), resourcePool);
        } catch (RuntimeException ex) {
            shardclient$blurFailed = true;
            ShardClientRuntime.LOGGER.error("Motion blur failed and was turned off", ex);
        }
    }
}
