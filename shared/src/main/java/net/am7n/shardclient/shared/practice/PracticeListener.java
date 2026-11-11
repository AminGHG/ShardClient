package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.protocol.packet.MatchStarted;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.protocol.packet.RoundEnded;
import net.am7n.shardclient.protocol.packet.TransitionStart;

/**
 * What the Practice Server told this client. The server decides when something happens; listeners decide how it
 * is shown. Every method runs on the client thread and does nothing by default.
 */
public interface PracticeListener {

    /** The connection to a Practice Server came up, went away or was refused. */
    default void onLinkChanged(PracticeLink.State state) {
    }

    default void onQueueJoined(QueueState queue) {
    }

    default void onQueueUpdated(QueueState queue) {
    }

    /** The player is out of {@code queue} without a match. */
    default void onQueueLeft(QueueState queue, QueueLeft.Reason reason) {
    }

    default void onMatchFound(MatchInfo match) {
    }

    /** The server is moving the player into {@code arena} (empty when unnamed). */
    default void onArenaTransition(String arena) {
    }

    default void onNotification(Notification notification) {
    }

    /** The server is about to move the player (into the arena or back to the lobby). */
    default void onTransitionStart(TransitionStart.Kind kind) {
    }

    /** A round begins with the player on their spawn. */
    default void onRoundStarted(MatchStarted round) {
    }

    default void onRoundEnded(RoundEnded round) {
    }

    /** The match is over; the result is usually followed by a return to the lobby. */
    default void onMatchEnded(MatchEnded result) {
    }

    default void onReturnToLobby() {
    }
}
