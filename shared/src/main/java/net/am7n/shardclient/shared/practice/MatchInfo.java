package net.am7n.shardclient.shared.practice;

import java.util.UUID;

public record MatchInfo(UUID opponentId, String opponentName, String mode, String kit, String arena,
                        boolean ranked, String opponentRank) {
}
