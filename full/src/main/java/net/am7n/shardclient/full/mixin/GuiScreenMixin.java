package net.am7n.shardclient.full.mixin;

import net.am7n.shardclient.full.menu.MenuScreens;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class GuiScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen shardclient$replaceScreen(Screen screen) {
        return MenuScreens.replace(screen);
    }
}
