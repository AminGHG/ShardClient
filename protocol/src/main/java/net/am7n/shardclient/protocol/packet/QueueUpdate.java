package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record QueueUpdate(String queueId, int waitSeconds, int playersQueued) implements ClientboundPacket {
    public static final int ID = 0x12;

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
        out.writeString(queueId, QueueJoined.MAX_QUEUE_ID_CHARS).writeInt(waitSeconds).writeInt(playersQueued);
    }

    static QueueUpdate read(PacketReader in) throws ProtocolException {
        return new QueueUpdate(in.readString(QueueJoined.MAX_QUEUE_ID_CHARS), in.readInt(), in.readInt());
    }
}
