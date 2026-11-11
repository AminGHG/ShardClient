package net.am7n.shardclient.protocol;

/** Which product a player runs. The wire id is what travels in the handshake; never reorder or reuse one. */
public enum ClientKind {

    FULL(1),
    LIGHT(2);

    private final int wireId;

    ClientKind(int wireId) {
        this.wireId = wireId;
    }

    public int wireId() {
        return wireId;
    }

    /** The kind for a wire id, or null when this build does not know it (a newer or broken client). */
    public static ClientKind byWireId(int wireId) {
        for (ClientKind kind : values()) {
            if (kind.wireId == wireId) {
                return kind;
            }
        }
        return null;
    }
}
