package net.am7n.shardclient.protocol;

public final class Protocol {
    public static final String NAMESPACE = "shardclient";
    public static final String CHANNEL_PATH = "main";

    public static final String CHANNEL = NAMESPACE + ":" + CHANNEL_PATH;

    public static final int VERSION_MIN = 1;

    public static final int VERSION_MAX = 1;

    public static final int MAX_CLIENTBOUND_BYTES = 16 * 1024;

    public static final int MAX_SERVERBOUND_BYTES = 1024;

    private Protocol() {
    }

    public static int negotiate(int peerMin, int peerMax) {
        if (peerMin < 1 || peerMax < peerMin) {
            return 0;
        }
        int version = Math.min(peerMax, VERSION_MAX);
        return version >= Math.max(peerMin, VERSION_MIN) ? version : 0;
    }
}
