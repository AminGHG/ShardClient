package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.NumberSetting;

public final class LowFire extends Module {
    private final NumberSetting lower = number("lower", "Lower by", 30, 5, 60, 5, "%");

    public LowFire() {
        super("low_fire", "Low fire", "Lowers the fire overlay while you burn.", Category.VISUAL);
    }

    public float offset() {
        return lower.floatValue() / 100f;
    }
}
