package net.am7n.shardclient.full.platform;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.function.Consumer;

final class Backend {
    private Backend() {
    }

    static void onRender(Consumer<World> handler) {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.consumers() != null && context.matrixStack() != null) {
                handler.accept(new World(context.consumers(), context.matrixStack(), context.camera()));
            }
        });
    }
}
