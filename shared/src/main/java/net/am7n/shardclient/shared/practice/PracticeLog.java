package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.shared.ShardClientRuntime;
import org.slf4j.Logger;

/** Writes Practice Server events to the game log, so a connection problem can be read from latest.log. */
final class PracticeLog implements PracticeListener {

    private static final Logger LOGGER = ShardClientRuntime.LOGGER;

    @Override
    public void onQueueJoined(QueueState queue) {
        LOGGER.info("Joined queue {} ({})", queue.id(), queue.ranked() ? "ranked" : "unranked");
    }

    @Override
    public void onQueueUpdated(QueueState queue) {
        LOGGER.debug("Queue {}: waited {}s, {} queued", queue.id(), queue.waitSeconds(), queue.playersQueued());
    }

    @Override
    public void onQueueLeft(QueueState queue, QueueLeft.Reason reason) {
        LOGGER.info("Left queue {} ({})", queue.id(), reason);
    }

    @Override
    public void onMatchFound(MatchInfo match) {
        LOGGER.info("Match found: {} vs {} in {}", match.mode(), match.opponentName(),
                match.arena().isEmpty() ? "?" : match.arena());
    }

    @Override
    public void onArenaTransition(String arena) {
        LOGGER.info("Entering arena {}", arena.isEmpty() ? "?" : arena);
    }

    @Override
    public void onNotification(Notification notification) {
        LOGGER.info("Notification [{} {}] {}: {}", notification.category(), notification.level(),
                notification.title(), notification.message());
    }
}
