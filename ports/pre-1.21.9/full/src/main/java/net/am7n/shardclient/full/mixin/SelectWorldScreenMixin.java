package net.am7n.shardclient.full.mixin;

import net.am7n.shardclient.full.menu.MenuScreens;
import net.am7n.shardclient.full.platform.WorldList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin implements MenuScreens.ScreenParent, WorldList.Host {
    @Shadow
    @Final
    protected Screen lastScreen;

    @Shadow
    private WorldSelectionList list;

    @Override
    public Screen shardclient$parent() {
        return lastScreen;
    }

    @Override
    public WorldSelectionList shardclient$list() {
        return list;
    }
}
