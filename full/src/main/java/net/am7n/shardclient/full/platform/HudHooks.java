package net.am7n.shardclient.full.platform;

import net.am7n.shardclient.shared.render.Assets;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;

import java.util.function.Predicate;

public final class HudHooks {
    private HudHooks() {
    }

    public static void beforeHud(String name, Runnable action) {
        HudElementRegistry.addFirst(Assets.id(Asset.of("shardclient", name)), (graphics, delta) -> action.run());
    }

    public static void crosshair(Predicate<Canvas> draw) {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
            if (!draw.test(Gfx.canvas(graphics, Minecraft.getInstance().font, -1, -1))) {
                original.extractRenderState(graphics, delta);
            }
        });
    }
}
