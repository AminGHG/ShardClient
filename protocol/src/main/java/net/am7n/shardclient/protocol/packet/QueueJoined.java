package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * The player is now waiting in a queue.
 *
 * @param queueId     stable id of the queue, e.g. {@code 1v1}; the same id comes back in updates and on leaving
 * @param displayName what to call the queue on screen
 */
public record QueueJoined(String queueId, String displayName, boolean ranked) implements ClientboundPacket {

    public static final int ID = 0x10;
    public static final int MAX_QUEUE_ID_CHARS = 32;
    public static final int MAX_NAME_CHARS = 48;

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Feature feature() {
        return Feature.MATCHMAKING;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeString(queueId, MAX_QUEUE_ID_CHARS).writeString(displayName, MAX_NAME_CHARS).writeBoolean(ranked);
    }

    static QueueJoined read(PacketReader in) throws ProtocolException {
        return new QueueJoined(in.readString(MAX_QUEUE_ID_CHARS), in.readString(MAX_NAME_CHARS), in.readBoolean());
    }
}
