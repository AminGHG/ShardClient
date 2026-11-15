package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record Welcome(int version, long features) implements ClientboundPacket {
    public static final int ID = 0x01;

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
        out.writeInt(version).writeLong(features);
    }

    static Welcome read(PacketReader in) throws ProtocolException {
        return new Welcome(in.readInt(), in.readLong());
    }
}
