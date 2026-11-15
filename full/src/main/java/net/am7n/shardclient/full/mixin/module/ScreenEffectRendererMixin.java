package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void shardclient$lowerFire(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite,
                                              CallbackInfo ci) {
        pose.pushPose();
        if (Features.on(Features.lowFire)) {
            pose.translate(0f, -Features.lowFire.offset(), 0f);
        }
    }

    @Inject(method = "submitFire", at = @At("RETURN"))
    private static void shardclient$lowerFireEnd(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite,
                                                 CallbackInfo ci) {
        pose.popPose();
    }
}
