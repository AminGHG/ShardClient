package net.am7n.shardclient.full.platform;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
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
        Asset texture = Asset.of(sprite.namespace(), "textures/gui/sprites/" + sprite.path() + ".png");
        Button button = new Button(0, 0, 20, 20, label, pressed -> press.run(), message -> message.get()) {
            @Override
            public void renderString(GuiGraphics graphics, Font font, int color) {
            }

            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                graphics.blit(Assets.id(texture), getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
            }
        };
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
