package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record TransitionStart(Kind kind) implements ClientboundPacket {
    public static final int ID = 0x30;

    public enum Kind {
        OTHER(0),

        ARENA(1),

        LOBBY(2);

        private final int wireId;

        Kind(int wireId) {
            this.wireId = wireId;
        }

        static Kind byWireId(int wireId) {
            for (Kind kind : values()) {
                if (kind.wireId == wireId) {
                    return kind;
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
        return Feature.MATCH_FLOW;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeByte(kind.wireId);
    }

    static TransitionStart read(PacketReader in) throws ProtocolException {
        return new TransitionStart(Kind.byWireId(in.readUnsignedByte()));
    }
}
