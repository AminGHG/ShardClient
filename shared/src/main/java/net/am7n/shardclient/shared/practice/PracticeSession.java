package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.ArenaTransition;
import net.am7n.shardclient.protocol.packet.ClientUsers;
import net.am7n.shardclient.protocol.packet.ClientboundPacket;
import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.protocol.packet.MatchFound;
import net.am7n.shardclient.protocol.packet.MatchStarted;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueJoined;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.protocol.packet.QueueUpdate;
import net.am7n.shardclient.protocol.packet.Rejected;
import net.am7n.shardclient.protocol.packet.ReturnToLobby;
import net.am7n.shardclient.protocol.packet.RoundEnded;
import net.am7n.shardclient.protocol.packet.TransitionStart;
import net.am7n.shardclient.protocol.packet.Welcome;
import net.am7n.shardclient.shared.ShardClientRuntime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * What this client currently knows about the player on the Practice Server: the queue they wait in and the match
 * found for them. Changed only by packets from the server; client thread only.
 */
public final class PracticeSession {

    private final List<PracticeListener> listeners = new CopyOnWriteArrayList<>();
    private QueueState queue;
    private MatchInfo match;

    public void addListener(PracticeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PracticeListener listener) {
        listeners.remove(listener);
    }

    /** The queue the player waits in, or null. */
    public QueueState queue() {
        return queue;
    }

    /** The match last found for the player on this connection, or null. */
    public MatchInfo match() {
        return match;
    }

    void handle(ClientboundPacket packet) {
        switch (packet) {
            case QueueJoined joined -> {
                queue = new QueueState(joined.queueId(), joined.displayName(), joined.ranked(), 0, 0);
                QueueState now = queue;
                fire(listener -> listener.onQueueJoined(now));
            }
            case QueueUpdate update -> {
                if (inQueue(update.queueId())) {
                    queue = queue.withProgress(update.waitSeconds(), update.playersQueued());
                    QueueState now = queue;
                    fire(listener -> listener.onQueueUpdated(now));
                }
            }
            case QueueLeft left -> {
                if (inQueue(left.queueId())) {
                    QueueState was = queue;
                    queue = null;
                    fire(listener -> listener.onQueueLeft(was, left.reason()));
                }
            }
            case MatchFound found -> {
                queue = null;
                match = new MatchInfo(found.opponentId(), found.opponentName(), found.mode(), found.kit(),
                        found.arena(), found.ranked(), found.opponentRank());
                MatchInfo now = match;
                fire(listener -> listener.onMatchFound(now));
            }
            case ArenaTransition transition -> fire(listener -> listener.onArenaTransition(transition.arena()));
            case Notification notification -> fire(listener -> listener.onNotification(notification));
            case TransitionStart start -> fire(listener -> listener.onTransitionStart(start.kind()));
            case MatchStarted started -> {
                queue = null;
                fire(listener -> listener.onRoundStarted(started));
            }
            case RoundEnded ended -> fire(listener -> listener.onRoundEnded(ended));
            case MatchEnded ended -> fire(listener -> listener.onMatchEnded(ended));
            case ReturnToLobby ignored -> {
                match = null;
                fire(PracticeListener::onReturnToLobby);
            }
            case ClientUsers users -> ClientBadges.apply(users);
            case Welcome ignored -> {
                // Handshake packets belong to PracticeLink.
            }
            case Rejected ignored -> {
            }
        }
    }

    /** Forgets everything: the connection ended or the server stopped talking to this client. */
    void clear() {
        ClientBadges.clear();
        queue = null;
        match = null;
    }

    void linkChanged(PracticeLink.State state) {
        fire(listener -> listener.onLinkChanged(state));
    }

    private boolean inQueue(String queueId) {
        return queue != null && queue.id().equals(queueId);
    }

    /** One broken listener must not stop the others or reach the network code. */
    private void fire(Consumer<PracticeListener> call) {
        for (PracticeListener listener : listeners) {
            try {
                call.accept(listener);
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.error("Practice listener {} failed", listener.getClass().getName(), ex);
            }
        }
    }
}
