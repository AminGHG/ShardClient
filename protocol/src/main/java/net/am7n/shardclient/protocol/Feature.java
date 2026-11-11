package net.am7n.shardclient.protocol;

/**
 * A group of packets a side can handle. Each side sends its set as a bit mask in the handshake; the server only
 * sends packets of features in both sets, so a new feature never reaches a client that predates it.
 */
public enum Feature {

    /** Queue joined / left / update, match found, arena transition. */
    MATCHMAKING(0),
    /** Server notifications. */
    NOTIFICATIONS(1),
    /** Full-screen transitions, match start, round and match results, return to lobby (ranked). */
    MATCH_FLOW(2),
    /** Which players on the server run ShardClient, for the badge in front of their names. */
    CLIENT_BADGES(3);

    private final long mask;

    Feature(int bit) {
        this.mask = 1L << bit;
    }

    public long mask() {
        return mask;
    }

    public boolean in(long features) {
        return (features & mask) != 0;
    }

    /** Every feature this build knows. */
    public static long all() {
        long features = 0;
        for (Feature feature : values()) {
            features |= feature.mask;
        }
        return features;
    }
}
