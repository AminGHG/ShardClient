package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * A round begins: the player is on their spawn in the arena. Scores are from this player's point of view.
 *
 * @param round      1 for the first round
 * @param winsNeeded rounds needed to win the match
 */
public record MatchStarted(int round, int winsNeeded, int scoreSelf, int scoreOpponent) implements ClientboundPacket {

    public static final int ID = 0x31;

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
        out.writeInt(round).writeInt(winsNeeded).writeInt(scoreSelf).writeInt(scoreOpponent);
    }

    static MatchStarted read(PacketReader in) throws ProtocolException {
        return new MatchStarted(in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }
}
