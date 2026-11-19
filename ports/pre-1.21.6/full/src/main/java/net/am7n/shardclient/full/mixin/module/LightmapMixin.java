package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class LightmapMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 0))
    private boolean shardclient$fullbright(LocalPlayer player, Holder<MobEffect> effect) {
        return player.hasEffect(effect) || Features.fullbright != null && Features.fullbright.isEnabled();
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float shardclient$fullbrightScale(LivingEntity entity, float partialTick) {
        float scale = entity.hasEffect(MobEffects.NIGHT_VISION) ? GameRenderer.getNightVisionScale(entity, partialTick) : 0f;
        if (Features.fullbright != null && Features.fullbright.isEnabled()) {
            return Math.max(scale, Features.fullbright.strength());
        }
        return scale;
    }
}
