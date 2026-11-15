package net.am7n.shardclient.showcase;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;

public final class ShowcaseMod implements ClientModInitializer {
    private boolean opened;

    @Override
    public void onInitializeClient() {
        boolean capture = Boolean.getBoolean("shardclient.showcase.capture");
        if (Boolean.getBoolean("shardclient.showcase.menus")) {
            MenuCapture.register();
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!opened && client.gui.overlay() == null && client.gui.screen() instanceof TitleScreen title) {
                opened = true;
                client.gui.setScreen(new ShowcaseScreen(title, capture));
            }
        });
    }
}
