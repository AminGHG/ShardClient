package net.am7n.shardclient.shared.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public abstract class ClientBrandMixin {
    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, remap = false)
    private static void shardclient$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("shardclient");
    }
}
