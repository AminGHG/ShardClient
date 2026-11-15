package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.protocol.ProtocolException;

public sealed interface ServerboundPacket permits Hello {
    int id();

    void write(PacketWriter out);

    default byte[] encode() {
        PacketWriter out = new PacketWriter().writeByte(id());
        write(out);
        return out.toByteArray();
    }

    static ServerboundPacket decode(byte[] message) throws ProtocolException {
        if (message.length > Protocol.MAX_SERVERBOUND_BYTES) {
            throw new ProtocolException("message of " + message.length + " bytes is too large");
        }
        PacketReader in = new PacketReader(message);
        return switch (in.readUnsignedByte()) {
            case Hello.ID -> Hello.read(in);
            default -> null;
        };
    }
}
