package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record QueueLeft(String queueId, Reason reason) implements ClientboundPacket {
    public static final int ID = 0x11;

    public enum Reason {
        OTHER(0),

        LEFT(1),

        CANCELLED(2);

        private final int wireId;

        Reason(int wireId) {
            this.wireId = wireId;
        }

        static Reason byWireId(int wireId) {
            for (Reason reason : values()) {
                if (reason.wireId == wireId) {
                    return reason;
                }
            }
            return OTHER;
        }
    }

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
        out.writeString(queueId, QueueJoined.MAX_QUEUE_ID_CHARS).writeByte(reason.wireId);
    }

    static QueueLeft read(PacketReader in) throws ProtocolException {
        return new QueueLeft(in.readString(QueueJoined.MAX_QUEUE_ID_CHARS), Reason.byWireId(in.readUnsignedByte()));
    }
}
