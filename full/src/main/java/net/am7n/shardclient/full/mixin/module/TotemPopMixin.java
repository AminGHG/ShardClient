package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.full.modules.pvp.CombatTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class TotemPopMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"))
    private void shardclient$totemPopped(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Entity entity = packet.getEntity(level);
        if (entity == null) {
            return;
        }
        CombatTracker.totemPopped(entity);
        if (entity instanceof Player player && Features.totemPopChams != null) {
            Features.totemPopChams.onPop(player);
        }
    }
}
