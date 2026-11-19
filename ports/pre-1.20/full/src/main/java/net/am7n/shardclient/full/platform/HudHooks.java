package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

import java.util.function.Predicate;

public final class HudHooks {
    private static Predicate<Canvas> crosshair;

    private HudHooks() {
    }

    public static void beforeHud(String name, Runnable action) {
        HudRenderCallback.EVENT.register((pose, delta) -> action.run());
    }

    public static void crosshair(Predicate<Canvas> draw) {
        crosshair = draw;
    }

    public static boolean drawCrosshair(PoseStack pose) {
        return crosshair != null && crosshair.test(Gfx.canvas(pose, Minecraft.getInstance().font, -1, -1));
    }
}
