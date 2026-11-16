package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @Inject(method = "lambda$grab$3", at = @At("TAIL"))
    private static void shardclient$copy(NativeImage image, File file, Consumer<Component> callback, CallbackInfo ci) {
        if (Features.on(Features.screenshotCopy)) {
            Features.screenshotCopy.saved(file);
        }
    }
}
