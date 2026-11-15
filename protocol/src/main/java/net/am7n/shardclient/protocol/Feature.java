package net.am7n.shardclient.protocol;

public enum Feature {
    MATCHMAKING(0),

    NOTIFICATIONS(1),

    MATCH_FLOW(2),

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

    public static long all() {
        long features = 0;
        for (Feature feature : values()) {
            features |= feature.mask;
        }
        return features;
    }
}
