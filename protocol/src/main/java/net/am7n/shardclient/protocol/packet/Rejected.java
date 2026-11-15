package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record Rejected(Reason reason, int serverMinVersion, int serverMaxVersion) implements ClientboundPacket {
    public static final int ID = 0x02;

    public enum Reason {
        OTHER(0),

        UNSUPPORTED_VERSION(1),

        DISABLED(2),

        INVALID_HELLO(3);

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
        return null;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeByte(reason.wireId).writeInt(serverMinVersion).writeInt(serverMaxVersion);
    }

    static Rejected read(PacketReader in) throws ProtocolException {
        return new Rejected(Reason.byWireId(in.readUnsignedByte()), in.readInt(), in.readInt());
    }
}
