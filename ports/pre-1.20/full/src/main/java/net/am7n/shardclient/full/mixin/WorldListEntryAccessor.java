package net.am7n.shardclient.full.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {
    @Accessor("iconLocation")
    ResourceLocation shardclient$icon();

    @Accessor("summary")
    LevelSummary shardclient$summary();
}
