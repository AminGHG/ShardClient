package net.am7n.shardclient.shared.practice;

/**
 * The queue the player is waiting in, as the server last described it.
 *
 * @param waitSeconds   time waited, counted by the server
 * @param playersQueued players in the queue, this one included; 0 until the first update
 */
public record QueueState(String id, String displayName, boolean ranked, int waitSeconds, int playersQueued) {

    QueueState withProgress(int waitSeconds, int playersQueued) {
        return new QueueState(id, displayName, ranked, waitSeconds, playersQueued);
    }
}
