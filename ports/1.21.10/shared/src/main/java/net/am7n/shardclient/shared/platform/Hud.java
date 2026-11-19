package net.am7n.shardclient.shared.platform;

import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class Hud {
    private Hud() {
    }

    public static void addLayer(String name, Consumer<Canvas> draw) {
        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("shardclient-shared", name),
                (graphics, delta) -> draw.accept(Gfx.canvas(graphics, Minecraft.getInstance().font, -1, -1)));
    }

    public static void afterEveryScreen(Consumer<Canvas> draw) {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) ->
                        draw.accept(Gfx.canvas(graphics, client.font, mouseX, mouseY))));
    }
}
