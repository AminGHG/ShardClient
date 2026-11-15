package net.am7n.shardclient.shared.practice;

public record QueueState(String id, String displayName, boolean ranked, int waitSeconds, int playersQueued) {
    QueueState withProgress(int waitSeconds, int playersQueued) {
        return new QueueState(id, displayName, ranked, waitSeconds, playersQueued);
    }
}
