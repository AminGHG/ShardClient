package net.am7n.shardclient.showcase;

import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/** Development only: the badge as the tab list and name tags draw it (the same decorated component). */
final class BadgePreviewScreen extends Screen {

    BadgePreviewScreen() {
        super(Component.literal("Badge preview"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xFF202020);
        Component[] names = {
                ClientBadges.decorate(Component.literal("Steve")),
                ClientBadges.decorate(Component.literal("Alex").withStyle(ChatFormatting.GOLD)),
                Component.literal("NoClient")
        };
        int y = 20;
        for (Component name : names) {
            graphics.text(font, name, 20, y, 0xFFFFFFFF, true);
            y += 14;
        }
    }
}
