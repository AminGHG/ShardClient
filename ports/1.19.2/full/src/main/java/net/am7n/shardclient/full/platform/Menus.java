package net.am7n.shardclient.full.platform;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
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
        widget.x = x;
        widget.y = y;
    }

    public static Screen options(Screen parent) {
        return new OptionsScreen(parent, Minecraft.getInstance().options);
    }

    public static AbstractWidget iconButton(Component label, Asset sprite, Runnable press) {
        Asset texture = Asset.of(sprite.namespace(), "textures/gui/sprites/" + sprite.path() + ".png");
        Button button = new Button(0, 0, 20, 20, Component.empty(), pressed -> press.run(),
                (widget, pose, mouseX, mouseY) -> {
                    Screen screen = Minecraft.getInstance().screen;
                    if (screen != null) {
                        screen.renderTooltip(pose, label, mouseX, mouseY);
                    }
                }) {
            @Override
            public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
                super.renderButton(pose, mouseX, mouseY, partialTick);
                RenderSystem.setShaderTexture(0, Assets.id(texture));
                GuiComponent.blit(pose, x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            }
        };
        return button;
    }

    public static Screen editServer(Screen parent, String title, ServerData server, BooleanConsumer done) {
        return new EditServerScreen(parent, done, server);
    }

    public static Screen directJoin(Screen parent, ServerData server, BooleanConsumer done) {
        return new DirectJoinServerScreen(parent, done, server);
    }
}
