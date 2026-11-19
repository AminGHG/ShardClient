package net.am7n.shardclient.shared.platform;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class Sounds {
    private Sounds() {
    }

    public static SoundEvent resonate() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }

    public static SoundEvent click() {
        return SoundEvents.UI_BUTTON_CLICK.value();
    }

    public static SoundEvent xylophone() {
        return SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
    }

    public static SoundEvent hat() {
        return SoundEvents.NOTE_BLOCK_HAT.value();
    }
}
