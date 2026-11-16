package net.am7n.shardclient.full.modules.pvp;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.full.mixin.module.OverlayTextureAccessor;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.minecraft.client.renderer.texture.DynamicTexture;

public final class HitColor extends Module {
    private final ChoiceSetting color = choice("color", "Colour", "Lilac", "Red", "Lilac", "Pink", "Blue", "Mint",
            "Gold", "White", "Black");
    private final NumberSetting strength = number("strength", "Strength", 40, 5, 90, 5, "%");

    private int original;
    private boolean saved;
    private int applied;
    private boolean changed;

    public HitColor() {
        super("hit_color", "Hit colour", "Changes the colour players and mobs flash when they take damage.",
                Category.PVP);
    }

    @Override
    public void tick() {
        apply(target());
    }

    @Override
    protected void onDisable() {
        if (changed && saved) {
            write(original);
            changed = false;
        }
    }

    private int target() {
        int rgb = switch (color.get()) {
            case "Red" -> 0xFF0000;
            case "Pink" -> 0xFF5FB4;
            case "Blue" -> 0x3D8BFF;
            case "Mint" -> 0x45F0A0;
            case "Gold" -> 0xFFC233;
            case "White" -> 0xFFFFFF;
            case "Black" -> 0x000000;
            default -> 0xB27BFF;
        };
        int keep = Math.round((1 - strength.floatValue() / 100f) * 255);
        return keep << 24 | rgb;
    }

    private void apply(int argb) {
        if (changed && argb == applied) {
            return;
        }
        if (write(argb)) {
            applied = argb;
            changed = true;
        }
    }

    private boolean write(int argb) {
        if (mc.gameRenderer == null) {
            return false;
        }
        DynamicTexture texture = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).shardclient$texture();
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return false;
        }
        if (!saved) {
            original = pixels.getPixel(0, 0);
            saved = true;
        }
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                pixels.setPixel(x, y, argb);
            }
        }
        texture.upload();
        return true;
    }
}
