package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class MotionBlur extends Module {
    private final ChoiceSetting strength = choice("strength", "Strength", "Medium", "Low", "Medium", "High", "Extreme");

    public MotionBlur() {
        super("motion_blur", "Motion blur", "Smooths fast camera movement by blending frames together.",
                Category.VISUAL);
    }

    public Identifier effect() {
        return Identifier.fromNamespaceAndPath("shardclient", "motion_blur_" + strength.get().toLowerCase(Locale.ROOT));
    }
}
