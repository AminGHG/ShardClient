package net.am7n.shardclient.full.mixin;

import net.am7n.shardclient.full.menu.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin implements MenuScreens.ScreenParent {
    @Shadow
    @Final
    private Screen lastScreen;

    @Override
    public Screen shardclient$parent() {
        return lastScreen;
    }
}
