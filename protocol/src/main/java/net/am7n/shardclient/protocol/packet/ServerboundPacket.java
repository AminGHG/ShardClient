package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.protocol.ProtocolException;

/** A message from a client to the server. Nothing in one is trusted: the server validates every field. */
public sealed interface ServerboundPacket permits Hello {

    int id();

    void write(PacketWriter out);

    default byte[] encode() {
        PacketWriter out = new PacketWriter().writeByte(id());
        write(out);
        return out.toByteArray();
    }

    /** The packet in {@code message}, or null when its id is unknown to this build (to be ignored). */
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
