package net.am7n.shardclient.shared.mixin;

import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the ShardClient badge in front of the names of players who run it, in the tab list. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void shardclient$badge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (ClientBadges.has(info.getProfile().id())) {
            cir.setReturnValue(ClientBadges.decorate(cir.getReturnValue()));
        }
    }
}
