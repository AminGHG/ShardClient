package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.NumberSetting;

public final class Fullbright extends Module {
    private final NumberSetting strength = number("strength", "Strength", 100, 10, 100, 5, "%");

    public Fullbright() {
        super("fullbright", "Fullbright", "See clearly in dark places, like night vision.", Category.VISUAL);
    }

    public float strength() {
        return strength.floatValue() / 100f;
    }
}
