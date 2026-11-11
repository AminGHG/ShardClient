package net.am7n.shardclient.showcase;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Opens the component gallery once the title screen is really showing (the loading overlay has gone), so its
 * entrance animation is visible. Development only.
 */
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
