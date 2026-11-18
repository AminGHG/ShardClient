package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.minecraft.resources.Identifier;

public final class MotionBlur extends Module {
    public static final Identifier EFFECT = Identifier.fromNamespaceAndPath("shardclient", "motion_blur");
    public static final String UNIFORM_BLOCK = "MotionBlurConfig";

    private final NumberSetting amount = number("amount", "Amount", 50, 0, 100, 1, "%");

    public MotionBlur() {
        super("motion_blur", "Motion blur", "Smooths fast camera movement by blending frames together.",
                Category.VISUAL);
    }

    public float blendFactor() {
        return Math.min(amount.intValue(), 99) / 100F;
    }

    public int amount() {
        return amount.intValue();
    }

    public void setAmountFromCommand(int percent) {
        amount.set((double) percent);
        if (percent > 0) {
            setEnabled(true);
        }
        ShardClientFull full = ShardClientFull.get();
        if (full != null && full.config() != null) {
            full.config().save();
        }
    }
}
