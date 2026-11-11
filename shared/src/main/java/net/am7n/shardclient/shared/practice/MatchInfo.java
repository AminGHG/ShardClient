package net.am7n.shardclient.shared.practice;

import java.util.UUID;

/**
 * The match the server found for the player. Text fields are empty when the server did not say.
 *
 * @param opponentId the opponent's profile id (their skin comes from the server's player list)
 */
public record MatchInfo(UUID opponentId, String opponentName, String mode, String kit, String arena,
                        boolean ranked, String opponentRank) {
}
