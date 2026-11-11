package net.am7n.shardclient.full.mixin;

import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The world's icon texture that Minecraft's list entry already loaded. */
@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {

    @Accessor("icon")
    FaviconTexture shardclient$icon();
}
