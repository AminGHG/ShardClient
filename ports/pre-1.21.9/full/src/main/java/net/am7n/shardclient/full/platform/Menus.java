package net.am7n.shardclient.full.platform;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
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
        SpriteIconButton button = SpriteIconButton.builder(label, pressed -> press.run(), true)
                .sprite(Assets.id(sprite), 16, 16).width(20).build();
        button.setTooltip(Tooltip.create(label));
        return button;
    }

    public static Screen editServer(Screen parent, String title, ServerData server, BooleanConsumer done) {
        return new EditServerScreen(parent, done, server);
    }

    public static Screen directJoin(Screen parent, ServerData server, BooleanConsumer done) {
        return new DirectJoinServerScreen(parent, done, server);
    }
}
