package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * The match is over, from this player's point of view. Rating values are 0 and rank texts empty when the match
 * was not rated (cancelled, unranked, placement).
 *
 * <p>Each rank comes with the rating range it covers, so the client can draw rating as progress through the
 * rank: {@code floor} is where the rank starts, {@code ceiling} where the next one starts (0 for the top rank).
 *
 * @param rankBefore the player's rank before the match, as plain text
 * @param rankAfter  the player's rank after the match, as plain text
 */
public record MatchEnded(Result result, int scoreSelf, int scoreOpponent, int ratingBefore, int ratingAfter,
                         String rankBefore, int beforeFloor, int beforeCeiling,
                         String rankAfter, int afterFloor, int afterCeiling) implements ClientboundPacket {

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

    /** A match without a rating change, e.g. cancelled. */
    public static MatchEnded unrated(Result result, int scoreSelf, int scoreOpponent) {
        return new MatchEnded(result, scoreSelf, scoreOpponent, 0, 0, "", 0, 0, "", 0, 0);
    }

    public boolean rated() {
        return ratingBefore != 0 || ratingAfter != 0;
    }

    public int ratingChange() {
        return ratingAfter - ratingBefore;
    }

    public boolean rankChanged() {
        return !rankBefore.isEmpty() && !rankAfter.isEmpty() && !rankBefore.equals(rankAfter);
    }

    /** Whether rating can be shown as progress through the rank (both ranges are known and non-empty). */
    public boolean hasRankProgress() {
        return rated() && afterCeiling > afterFloor && (!rankChanged() || beforeCeiling > beforeFloor);
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
                .writeString(rankBefore, MAX_RANK_CHARS).writeInt(beforeFloor).writeInt(beforeCeiling)
                .writeString(rankAfter, MAX_RANK_CHARS).writeInt(afterFloor).writeInt(afterCeiling);
    }

    static MatchEnded read(PacketReader in) throws ProtocolException {
        return new MatchEnded(Result.byWireId(in.readUnsignedByte()), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(),
                in.readString(MAX_RANK_CHARS), in.readInt(), in.readInt(),
                in.readString(MAX_RANK_CHARS), in.readInt(), in.readInt());
    }
}
