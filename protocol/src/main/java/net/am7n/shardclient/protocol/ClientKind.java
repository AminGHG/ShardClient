package net.am7n.shardclient.protocol;

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

    public static ClientKind byWireId(int wireId) {
        for (ClientKind kind : values()) {
            if (kind.wireId == wireId) {
                return kind;
            }
        }
        return null;
    }
}
