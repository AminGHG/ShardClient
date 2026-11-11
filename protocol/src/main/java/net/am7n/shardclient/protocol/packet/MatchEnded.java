package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * The match is over, from this player's point of view. Rating values are 0 and rank texts empty when the match
 * was not rated (cancelled, or unranked).
 *
 * @param rankAfter   the player's rank after the match, as plain text
 * @param rankChanged whether the match moved the player to another rank
 */
public record MatchEnded(Result result, int scoreSelf, int scoreOpponent, int ratingBefore, int ratingAfter,
                         String rankAfter, boolean rankChanged) implements ClientboundPacket {

    public static final int ID = 0x33;
    public static final int MAX_RANK_CHARS = 48;

    public enum Result {
        /** Anything this build has no name for. */
        OTHER(0),
        WIN(1),
        LOSS(2),
        /** Stopped without a result (setup failed, server stopping, ...). */
        CANCELLED(3);

        private final int wireId;

        Result(int wireId) {
            this.wireId = wireId;
        }

        static Result byWireId(int wireId) {
            for (Result result : values()) {
                if (result.wireId == wireId) {
                    return result;
                }
            }
            return OTHER;
        }
    }

    public int ratingChange() {
        return ratingAfter - ratingBefore;
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
        out.writeByte(result.wireId).writeInt(scoreSelf).writeInt(scoreOpponent)
                .writeInt(ratingBefore).writeInt(ratingAfter)
                .writeString(rankAfter, MAX_RANK_CHARS).writeBoolean(rankChanged);
    }

    static MatchEnded read(PacketReader in) throws ProtocolException {
        return new MatchEnded(Result.byWireId(in.readUnsignedByte()), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readString(MAX_RANK_CHARS), in.readBoolean());
    }
}
