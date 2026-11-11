package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.protocol.packet.MatchStarted;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.RoundEnded;
import net.am7n.shardclient.protocol.packet.TransitionStart;
import net.am7n.shardclient.shared.render.Theme;
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
            if (resultCard != null && resultCard.isShown()) {
                return MatchResultCard.TOP + MatchResultCard.HEIGHT;
            }
            if (matchCard != null && matchCard.isShown()) {
                return MatchFoundCard.BOTTOM;
            }
            if (banner != null && banner.isShown()) {
                return TopBanner.TOP + TopBanner.HEIGHT;
            }
            return queue.isShown() ? QueueIndicator.BOTTOM : 0;
        }
    });
    private MatchFoundCard matchCard;
    private final ScreenTransition transition = new ScreenTransition();
    private TopBanner banner;
    private MatchResultCard resultCard;
    /** The last match result, shown once the player is back in the lobby. */
    private MatchEnded pendingResult;

    public PracticeOverlays(OverlayHost host) {
        this.host = host;
    }

    @Override
    public void onLinkChanged(PracticeLink.State state) {
        if (state != PracticeLink.State.ACTIVE) {
            host.clear();
            queue.reset();
            toasts.clear();
            transition.reset();
            matchCard = null;
            banner = null;
            resultCard = null;
            pendingResult = null;
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
    public void onTransitionStart(TransitionStart.Kind kind) {
        // The cover hides everything; what was on screen has been seen.
        if (matchCard != null) {
            matchCard.dismiss();
        }
        dismissBanner();
        transition.cover();
        host.addOnTop(transition);
    }

    @Override
    public void onRoundStarted(MatchStarted round) {
        queue.hide();
        transition.reveal();
        String detail = round.round() == 1
                ? "First to " + round.winsNeeded()
                : round.scoreSelf() + "  –  " + round.scoreOpponent();
        showBanner(new TopBanner("Round " + round.round(), detail, Theme.colors().accentStrong(), 2200));
    }

    @Override
    public void onRoundEnded(RoundEnded round) {
        String score = round.scoreSelf() + "  –  " + round.scoreOpponent();
        if (round.won()) {
            showBanner(new TopBanner("Round won", score, Theme.colors().positive(), 2600).pulse());
            UiSounds.roundWon();
        } else {
            showBanner(new TopBanner("Round lost", score, Theme.colors().negative(), 2600));
            UiSounds.roundLost();
        }
    }

    @Override
    public void onMatchEnded(MatchEnded result) {
        pendingResult = result;
    }

    @Override
    public void onReturnToLobby() {
        transition.reveal();
        dismissBanner();
        if (pendingResult == null) {
            return;
        }
        if (resultCard != null) {
            resultCard.dismiss();
        }
        resultCard = new MatchResultCard(pendingResult);
        host.add(resultCard);
        if (transition.isActive()) {
            host.addOnTop(transition);
        }
        switch (pendingResult.result()) {
            case WIN -> UiSounds.matchWon();
            case LOSS -> UiSounds.matchLost();
            default -> UiSounds.queueLeft();
        }
        pendingResult = null;
    }

    private void showBanner(TopBanner next) {
        dismissBanner();
        banner = next;
        host.add(banner);
        if (transition.isActive()) {
            host.addOnTop(transition);
        }
    }

    private void dismissBanner() {
        if (banner != null) {
            banner.dismiss();
        }
    }

    @Override
    public void onNotification(Notification notification) {
        toasts.push(notification);
        host.add(toasts);
        UiSounds.notification(notification.level());
    }
}
