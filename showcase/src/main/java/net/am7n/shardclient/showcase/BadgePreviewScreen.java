package net.am7n.shardclient.showcase;

import net.am7n.shardclient.shared.practice.ClientBadges;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

final class BadgePreviewScreen extends Screen {
    BadgePreviewScreen() {
        super(Component.literal("Badge preview"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xFF202020);
        Component[] names = {
                ClientBadges.decorate(Component.literal("Steve"), "Steve"),
                ClientBadges.decorate(Component.literal("Alex").withStyle(ChatFormatting.GOLD), "Alex"),
                Component.literal("NoClient")
        };
        int y = 20;
        for (Component name : names) {
            graphics.text(font, name, 20, y, 0xFFFFFFFF, true);
            y += 14;
        }

        String[] fonts = {"caveat", "dancing_script", "satisfy", "permanent_marker", "bebas_neue", "anton", "righteous",
                "space_mono"};
        y = 20;
        for (String id : fonts) {
            var style = net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription
                    .Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("shardclient", "lyrics_" + id)));
            graphics.text(font, Component.literal(id + ": Now we're standing side by side ÄÖÜß").withStyle(style),
                    130, y, 0xFFFFFFFF, true);
            y += 16;
        }
    }
}
