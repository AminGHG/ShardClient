package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.protocol.packet.MatchStarted;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.protocol.packet.RoundEnded;
import net.am7n.shardclient.protocol.packet.TransitionStart;

public interface PracticeListener {
    default void onLinkChanged(PracticeLink.State state) {
    }

    default void onQueueJoined(QueueState queue) {
    }

    default void onQueueUpdated(QueueState queue) {
    }

    default void onQueueLeft(QueueState queue, QueueLeft.Reason reason) {
    }

    default void onMatchFound(MatchInfo match) {
    }

    default void onArenaTransition(String arena) {
    }

    default void onNotification(Notification notification) {
    }

    default void onTransitionStart(TransitionStart.Kind kind) {
    }

    default void onRoundStarted(MatchStarted round) {
    }

    default void onRoundEnded(RoundEnded round) {
    }

    default void onMatchEnded(MatchEnded result) {
    }

    default void onReturnToLobby() {
    }
}
