package net.am7n.shardclient.showcase;

import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.ItemIcon;
import net.am7n.shardclient.shared.ui.Label;
import net.am7n.shardclient.shared.ui.Panel;
import net.am7n.shardclient.shared.ui.PlayerFace;
import net.am7n.shardclient.shared.ui.ProgressBar;
import net.am7n.shardclient.shared.ui.ScrollArea;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Spacer;
import net.am7n.shardclient.shared.ui.Stack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** One of every shared component, to judge spacing, textures and animation in a real game window. */
final class ShowcaseScreen extends ShardScreen {

    private final ShowcaseCapture capture;

    ShowcaseScreen(Screen parent, boolean capture) {
        super(Component.literal("ShardClient components"), parent);
        this.capture = capture ? new ShowcaseCapture(this) : null;
    }

    @Override
    protected Element build() {
        Stack modes = Stack.row(Theme.GAP).align(Stack.Align.CENTER)
                .add(new ItemIcon(item(Items.DIAMOND_SWORD)).inSlot())
                .add(new ItemIcon(item(Items.MACE)).inSlot())
                .add(new ItemIcon(item(Items.SPLASH_POTION)).inSlot())
                .add(new ItemIcon(item(Items.END_CRYSTAL)).inSlot())
                .add(new Spacer(Theme.GAP, 0))
                .add(new Label("Item icons in slots").color(() -> Theme.colors().textMuted()));

        Stack players = Stack.row(Theme.GAP_LARGE).align(Stack.Align.CENTER)
                .add(new PlayerFace(UUID.fromString("00000000-0000-0000-0000-000000000001"), 24))
                .add(new Label("Steve_01"))
                .add(Spacer.flexible())
                .add(new Label("VS").color(() -> Theme.colors().accentStrong()))
                .add(Spacer.flexible())
                .add(new Label("Alex_02"))
                .add(new PlayerFace(UUID.fromString("00000000-0000-0000-0000-000000000002"), 24));

        Stack rows = Stack.column(Theme.GAP);
        for (int i = 1; i <= 12; i++) {
            rows.add(new Label("Scrollable row " + i).color(i % 2 == 0 ? () -> Theme.colors().textMuted() : () -> Theme.colors().text()));
        }

        Stack buttons = Stack.row(Theme.GAP)
                .add(new Button("Confirm", () -> { }).style(Button.Style.PRIMARY).grow(1))
                .add(new Button("Disabled", () -> { }).enabled(false).grow(1))
                .add(new Button("Close", this::onClose).grow(1));

        Stack content = Stack.column(Theme.GAP_LARGE)
                .add(new Label("ShardClient components").emphasis().align(Label.Align.CENTER))
                .add(new Label("Shared by the Full Client and the Light Version")
                        .color(() -> Theme.colors().textMuted()).align(Label.Align.CENTER))
                .add(modes)
                .add(players)
                .add(new ProgressBar(220).progress(0.65f))
                .add(new ProgressBar(220).indeterminate())
                .add(new ScrollArea(rows, 44))
                .add(buttons);

        return new Panel(content).minWidth(264);
    }

    /** Item stacks only exist once a world is loaded; on the title screen the slots stay empty. */
    private ItemStack item(Item item) {
        return minecraft.level == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (capture != null) {
            capture.frame();
        }
    }
}
