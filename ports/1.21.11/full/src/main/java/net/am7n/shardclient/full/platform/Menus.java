package net.am7n.shardclient.full.platform;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class Menus {
    private Menus() {
    }

    public static void addWidget(Screen screen, AbstractWidget widget) {
        Screens.getButtons(screen).add(widget);
    }

    public static void place(AbstractWidget widget, int x, int y) {
        widget.setPosition(x, y);
    }

    public static Screen options(Screen parent) {
        return new OptionsScreen(parent, Minecraft.getInstance().options);
    }

    public static AbstractWidget iconButton(Component label, Asset sprite, Runnable press) {
        return SpriteIconButton.builder(label, button -> press.run(), true)
                .sprite(Assets.id(sprite), 16, 16).width(20).withTootip().build();
    }

    public static Screen editServer(Screen parent, String title, ServerData server, BooleanConsumer done) {
        return new ManageServerScreen(parent, Component.literal(title), done, server);
    }

    public static Screen directJoin(Screen parent, ServerData server, BooleanConsumer done) {
        return new DirectJoinServerScreen(parent, done, server);
    }
}
