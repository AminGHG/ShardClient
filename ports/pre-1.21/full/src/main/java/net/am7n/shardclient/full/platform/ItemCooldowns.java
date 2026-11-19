package net.am7n.shardclient.full.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ItemCooldowns {
    private ItemCooldowns() {
    }

    public static List<ItemStack> tracked() {
        return List.of(new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.SHIELD),
                new ItemStack(Items.CHORUS_FRUIT));
    }

    public static Component effectName(MobEffectInstance effect) {
        return effect.getEffect().value().getDisplayName();
    }

    public static boolean infinite(MobEffectInstance effect) {
        return effect.isInfiniteDuration();
    }

    public static boolean active(Player player, ItemStack stack) {
        return player.getCooldowns().isOnCooldown(stack.getItem());
    }

    public static float percent(Player player, ItemStack stack, float partialTick) {
        return player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
    }
}
