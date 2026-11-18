package net.am7n.shardclient.shared.mixin;

import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayMixin {
    @Shadow
    private Display.TextDisplay.CachedInfo clientDisplayCache;

    @Unique
    private int shardclient$badgeVersion = -1;

    @Inject(method = "cacheDisplay", at = @At("HEAD"))
    private void shardclient$refreshBadges(Display.TextDisplay.LineSplitter splitter,
                                           CallbackInfoReturnable<Display.TextDisplay.CachedInfo> cir) {
        int current = ClientBadges.version();
        if (shardclient$badgeVersion != current) {
            shardclient$badgeVersion = current;
            clientDisplayCache = null;
        }
    }

    @ModifyArg(method = "cacheDisplay", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Display$TextDisplay$LineSplitter;split(Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;"),
            index = 0)
    private Component shardclient$badge(Component text) {
        return ClientBadges.decorateAny(text);
    }
}
