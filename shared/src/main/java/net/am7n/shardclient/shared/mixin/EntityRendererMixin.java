package net.am7n.shardclient.shared.mixin;

import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the ShardClient badge in front of the name tag above players who run it. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void shardclient$badge(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name != null && entity instanceof Player && ClientBadges.has(entity.getUUID())) {
            cir.setReturnValue(ClientBadges.decorate(name));
        }
    }
}
