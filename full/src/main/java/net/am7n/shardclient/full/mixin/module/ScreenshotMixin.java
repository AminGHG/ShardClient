package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @Inject(method = "getFile", at = @At("RETURN"))
    private static void shardclient$copy(File directory, CallbackInfoReturnable<File> cir) {
        if (Features.on(Features.screenshotCopy)) {
            Features.screenshotCopy.expect(cir.getReturnValue());
        }
    }
}
