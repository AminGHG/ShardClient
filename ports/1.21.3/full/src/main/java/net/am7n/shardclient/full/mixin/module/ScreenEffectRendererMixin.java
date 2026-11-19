package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void shardclient$lowerFire(Minecraft minecraft, PoseStack pose,
                                              CallbackInfo ci) {
        pose.pushPose();
        if (Features.on(Features.lowFire)) {
            pose.translate(0f, -Features.lowFire.offset(), 0f);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void shardclient$lowerFireEnd(Minecraft minecraft, PoseStack pose,
                                                 CallbackInfo ci) {
        pose.popPose();
    }
}
