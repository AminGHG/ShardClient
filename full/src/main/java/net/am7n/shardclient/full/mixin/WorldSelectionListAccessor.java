package net.am7n.shardclient.full.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(WorldSelectionList.class)
public interface WorldSelectionListAccessor {
    @Invoker("pollLevelsIgnoreErrors")
    List<LevelSummary> shardclient$pollLevels();

    @Invoker("handleNewLevels")
    void shardclient$handleNewLevels(List<LevelSummary> levels);

    @Accessor("currentlyDisplayedLevels")
    List<LevelSummary> shardclient$displayedLevels();
}
