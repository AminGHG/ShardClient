package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Which players on this server run ShardClient, so the client can show a badge in front of their names.
 *
 * @param replace true: {@code added} is the complete list (sent once after the handshake); false: a change
 * @param added   players now running ShardClient
 * @param removed players who left or no longer run it
 */
public record ClientUsers(boolean replace, List<UUID> added, List<UUID> removed) implements ClientboundPacket {

    public static final int ID = 0x40;
    /** Keeps one message well under the size limit; a longer list is sent in several. */
    public static final int MAX_PER_LIST = 500;

    public ClientUsers {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Feature feature() {
        return Feature.CLIENT_BADGES;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeBoolean(replace);
        writeList(out, added);
        writeList(out, removed);
    }

    private static void writeList(PacketWriter out, List<UUID> ids) {
        int count = Math.min(ids.size(), MAX_PER_LIST);
        out.writeInt(count);
        for (int i = 0; i < count; i++) {
            out.writeUuid(ids.get(i));
        }
    }

    static ClientUsers read(PacketReader in) throws ProtocolException {
        boolean replace = in.readBoolean();
        return new ClientUsers(replace, readList(in), readList(in));
    }

    private static List<UUID> readList(PacketReader in) throws ProtocolException {
        int count = in.readInt();
        if (count < 0 || count > MAX_PER_LIST) {
            throw new ProtocolException("list of " + count + " players");
        }
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(in.readUuid());
        }
        return ids;
    }
}
