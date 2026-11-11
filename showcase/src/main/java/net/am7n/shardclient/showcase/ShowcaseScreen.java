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
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.practice.QueueState;
import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.practice.ui.MatchFoundCard;
import net.am7n.shardclient.shared.practice.ui.MatchResultCard;
import net.am7n.shardclient.shared.practice.ui.ScreenTransition;
import net.am7n.shardclient.shared.practice.ui.TopBanner;
import net.am7n.shardclient.shared.practice.ui.NotificationToasts;
import net.am7n.shardclient.shared.practice.ui.QueueIndicator;
import net.am7n.shardclient.shared.render.Canvas;
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
    /** Overlay previews drawn over the screen, with sample data; nothing goes through the network. */
    private final OverlayHost previews = new OverlayHost();
    private boolean previewing;
    private final ScreenTransition transition = new ScreenTransition();

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

    /** Shows the Practice Server overlays with sample data in place of the component gallery. */
    void previewOverlays() {
        previewing = true;
        previews.clear();
        QueueIndicator queue = new QueueIndicator();
        queue.show(new QueueState("1v1", "1v1", false, 42, 3));
        previews.add(queue);
        previews.add(new MatchFoundCard(
                new MatchInfo(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Alex_02", "1v1", "",
                        "badlands", false, ""),
                UUID.fromString("00000000-0000-0000-0000-000000000001"), "Steve_01"));
        NotificationToasts toasts = new NotificationToasts(new NotificationToasts.TopCenter() {
            @Override
            public int width() {
                return MatchFoundCard.CARD_WIDTH;
            }

            @Override
            public int bottom() {
                return MatchFoundCard.BOTTOM;
            }
        });
        toasts.push(new Notification(Notification.Category.PLAYER, Notification.Level.INFO, "Duel request",
                "Alex_02 wants to duel you. Type /duelaccept to fight."));
        toasts.push(new Notification(Notification.Category.ANNOUNCEMENT, Notification.Level.SUCCESS, "",
                "Ranked season 3 starts this weekend."));
        previews.add(toasts);
    }

    /** Shows the match result card and a round banner with sample data. */
    void previewResult() {
        previewing = true;
        previews.clear();
        previews.add(new MatchResultCard(new MatchEnded(MatchEnded.Result.WIN, 2, 1, 1172, 1214, "Gold I", 1100, 1200, "Gold II", 1200, 1300)));
    }

    /** Covers the screen like an arena transition; {@link #previewReveal()} lifts it. */
    void previewTransition() {
        previewing = true;
        previews.clear();
        previews.add(new TopBanner("Round 1", "First to 2", Theme.colors().accentStrong(), 60_000));
        transition.cover();
        previews.addOnTop(transition);
    }

    void previewReveal() {
        transition.reveal();
    }

    /** Item stacks only exist once a world is loaded; on the title screen the slots stay empty. */
    private ItemStack item(Item item) {
        return minecraft.level == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (previewing) {
            previews.render(new Canvas(graphics, font, mouseX, mouseY));
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (capture != null) {
            capture.frame();
        }
    }
}
