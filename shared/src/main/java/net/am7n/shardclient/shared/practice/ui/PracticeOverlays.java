package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.practice.PracticeLink;
import net.am7n.shardclient.shared.practice.PracticeListener;
import net.am7n.shardclient.shared.practice.QueueState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Turns what the server says into what the player sees: the queue indicator, the Match Found card and
 * notifications. Nothing here starts on its own; every change follows a server event. When the connection to the
 * Practice Server goes away or is refused, everything is cleared at once so nothing is left on screen.
 */
public final class PracticeOverlays implements PracticeListener {

    private final OverlayHost host;
    private final QueueIndicator queue = new QueueIndicator();
    private final NotificationToasts toasts = new NotificationToasts(new NotificationToasts.TopCenter() {
        @Override
        public int width() {
            return Math.max(QueueIndicator.CARD_WIDTH, MatchFoundCard.CARD_WIDTH);
        }

        @Override
        public int bottom() {
            if (matchCard != null && matchCard.isShown()) {
                return MatchFoundCard.BOTTOM;
            }
            return queue.isShown() ? QueueIndicator.BOTTOM : 0;
        }
    });
    private MatchFoundCard matchCard;

    public PracticeOverlays(OverlayHost host) {
        this.host = host;
    }

    @Override
    public void onLinkChanged(PracticeLink.State state) {
        if (state != PracticeLink.State.ACTIVE) {
            host.clear();
            queue.reset();
            toasts.clear();
            matchCard = null;
        }
    }

    @Override
    public void onQueueJoined(QueueState state) {
        queue.show(state);
        host.add(queue);
        UiSounds.queueJoined();
    }

    @Override
    public void onQueueUpdated(QueueState state) {
        queue.update(state);
    }

    @Override
    public void onQueueLeft(QueueState state, QueueLeft.Reason reason) {
        queue.hide();
        UiSounds.queueLeft();
    }

    @Override
    public void onMatchFound(MatchInfo match) {
        queue.hide();
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null) {
            return;
        }
        if (matchCard != null) {
            matchCard.dismiss();
        }
        matchCard = new MatchFoundCard(match, self.getUUID(), self.getScoreboardName());
        host.add(matchCard);
        UiSounds.matchFound();
    }

    @Override
    public void onArenaTransition(String arena) {
        if (matchCard != null) {
            matchCard.enteringArena(arena);
        }
    }

    @Override
    public void onNotification(Notification notification) {
        toasts.push(notification);
        host.add(toasts);
        UiSounds.notification(notification.level());
    }
}
