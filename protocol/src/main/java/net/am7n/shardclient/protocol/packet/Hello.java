package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record Hello(int minVersion, int maxVersion, int kindId, String clientVersion, long features)
        implements ServerboundPacket {
    public static final int ID = 0x01;
    public static final int MAX_VERSION_CHARS = 32;

    public Hello(int minVersion, int maxVersion, ClientKind kind, String clientVersion, long features) {
        this(minVersion, maxVersion, kind.wireId(), clientVersion, features);
    }

    public ClientKind kind() {
        return ClientKind.byWireId(kindId);
    }

    public String printableClientVersion() {
        StringBuilder out = new StringBuilder(clientVersion.length());
        for (int i = 0; i < clientVersion.length(); i++) {
            char c = clientVersion.charAt(i);
            out.append(c > 0x20 && c < 0x7F ? c : '?');
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeInt(minVersion).writeInt(maxVersion).writeByte(kindId)
                .writeString(clientVersion, MAX_VERSION_CHARS).writeLong(features);
    }

    static Hello read(PacketReader in) throws ProtocolException {
        return new Hello(in.readInt(), in.readInt(), in.readUnsignedByte(),
                in.readString(MAX_VERSION_CHARS), in.readLong());
    }
}
