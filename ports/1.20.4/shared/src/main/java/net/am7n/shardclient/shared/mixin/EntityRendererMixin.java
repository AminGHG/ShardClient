package net.am7n.shardclient.shared.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.shared.platform.Profiles;
import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component shardclient$badge(Component name, Entity entity, Component displayName, PoseStack pose,
                                        MultiBufferSource buffers, int light) {
        if (name == null) {
            return null;
        }
        if (entity instanceof Player player) {
            return ClientBadges.has(player.getUUID())
                    ? ClientBadges.decorate(name, Profiles.name(player.getGameProfile())) : name;
        }
        return ClientBadges.decorateAny(name);
    }
}
