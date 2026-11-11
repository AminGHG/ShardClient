package net.am7n.shardclient.protocol;

/**
 * The ShardClient wire protocol: one plugin channel on the normal Minecraft connection, carrying
 * {@code [packet id: 1 byte][body]} in both directions.
 *
 * <p>Compatibility rules, which every reader and writer in this module follows:
 * <ul>
 *   <li>An unknown packet id is ignored, never an error.</li>
 *   <li>New fields are only ever appended to a packet; readers ignore bytes after the fields they know.</li>
 *   <li>A field never changes meaning. Anything that would break an older reader raises {@link #VERSION_MAX}.</li>
 *   <li>Both sides announce a version range and a feature set; the server picks the highest common version and
 *       only sends packets of features both sides announced.</li>
 * </ul>
 */
public final class Protocol {

    public static final String NAMESPACE = "shardclient";
    public static final String CHANNEL_PATH = "main";
    /** The plugin channel, as Bukkit and Minecraft write it. */
    public static final String CHANNEL = NAMESPACE + ":" + CHANNEL_PATH;

    /** Oldest protocol version this build can still speak. */
    public static final int VERSION_MIN = 1;
    /** Newest protocol version this build speaks. */
    public static final int VERSION_MAX = 1;

    /** Upper bound for a message from the server; anything larger is dropped unread. */
    public static final int MAX_CLIENTBOUND_BYTES = 16 * 1024;
    /** Upper bound for a message from a client; anything larger is dropped unread. */
    public static final int MAX_SERVERBOUND_BYTES = 1024;

    private Protocol() {
    }

    /**
     * The version both sides will speak: the highest one inside both ranges, or 0 when the ranges do not overlap
     * or the peer's range is nonsense.
     */
    public static int negotiate(int peerMin, int peerMax) {
        if (peerMin < 1 || peerMax < peerMin) {
            return 0;
        }
        int version = Math.min(peerMax, VERSION_MAX);
        return version >= Math.max(peerMin, VERSION_MIN) ? version : 0;
    }
}
