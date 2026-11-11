package net.am7n.shardclient.full.mixin;

import net.am7n.shardclient.full.menu.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Gives the ShardClient world list the screen the vanilla one would have gone back to. */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin implements MenuScreens.ScreenParent {

    @Shadow
    @Final
    protected Screen lastScreen;

    @Override
    public Screen shardclient$parent() {
        return lastScreen;
    }
}
