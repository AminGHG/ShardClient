package net.am7n.shardclient.full.menu;

import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.am7n.shardclient.full.platform.Menus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class MenuScreens {
    private static final Asset TOGGLE_SPRITE = Asset.shared("icon/shardclient");

    private MenuScreens() {
    }

    private static boolean shardStyle() {
        ShardClientFull client = ShardClientFull.get();
        return client != null && client.config() != null
                && client.config().menuStyle() == ClientConfig.MenuStyle.SHARD;
    }

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
            if (screen.getClass() == SelectWorldScreen.class) {
                Screen parent = ((ScreenParent) screen).shardclient$parent();
                return new ShardWorldListScreen(parent);
            }
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.error("Could not open the ShardClient menu; showing Minecraft's", ex);
        }
        return screen;
    }

    public static void toggle() {
        ShardClientFull client = ShardClientFull.get();
        ClientConfig config = client.config();
        config.setMenuStyle(config.menuStyle() == ClientConfig.MenuStyle.SHARD
                ? ClientConfig.MenuStyle.VANILLA : ClientConfig.MenuStyle.SHARD);
        Game.setScreen(new TitleScreen());
    }

    public static void registerVanillaToggle() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen.getClass() == TitleScreen.class) {
                Component label = Component.literal("Switch to ShardClient's menu");
                AbstractWidget button = Menus.iconButton(label, TOGGLE_SPRITE, MenuScreens::toggle);
                Menus.place(button, 4, height - 36);
                Menus.addWidget(screen, button);
            }
        });
    }

    public interface ScreenParent {
        Screen shardclient$parent();
    }
}
