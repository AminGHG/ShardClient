package net.am7n.shardclient.full.menu;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

/**
 * Which menu style is shown: ShardClient's title screen and server list, or Minecraft's. The vanilla title
 * screen gets a button in its bottom-left corner to switch back, just like ShardClient's has one to switch away.
 */
public final class MenuScreens {

    private MenuScreens() {
    }

    private static boolean shardStyle() {
        ShardClientFull client = ShardClientFull.get();
        return client != null && client.config() != null
                && client.config().menuStyle() == ClientConfig.MenuStyle.SHARD;
    }

    /** The screen to show instead of {@code screen}; {@code screen} itself in vanilla style or if anything fails. */
    public static Screen replace(Screen screen) {
        if (screen == null || !shardStyle()) {
            return screen;
        }
        try {
            if (screen.getClass() == TitleScreen.class) {
                return new ShardTitleScreen();
            }
            if (screen.getClass() == JoinMultiplayerScreen.class) {
                Screen parent = ((ScreenParent) screen).shardclient$parent();
                return new ShardServerListScreen(parent);
            }
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.error("Could not open the ShardClient menu; showing Minecraft's", ex);
        }
        return screen;
    }

    /** Switches the style and shows the matching title screen right away. */
    public static void toggle() {
        ShardClientFull client = ShardClientFull.get();
        ClientConfig config = client.config();
        config.setMenuStyle(config.menuStyle() == ClientConfig.MenuStyle.SHARD
                ? ClientConfig.MenuStyle.VANILLA : ClientConfig.MenuStyle.SHARD);
        Minecraft.getInstance().gui.setScreen(new TitleScreen());
    }

    /** Adds the style switch to Minecraft's own title screen. */
    public static void registerVanillaToggle() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen.getClass() == TitleScreen.class) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal("ShardClient style"),
                        button -> toggle()).bounds(4, height - 36, 110, 20).build());
            }
        });
    }

    /** Lets the server list replacement keep the screen the vanilla list would have returned to. */
    public interface ScreenParent {
        Screen shardclient$parent();
    }
}
