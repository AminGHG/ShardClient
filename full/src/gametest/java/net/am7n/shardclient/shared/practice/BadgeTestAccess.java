package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.ClientUsers;

import java.util.List;
import java.util.UUID;

public final class BadgeTestAccess {
    private BadgeTestAccess() {
    }

    public static void give(UUID player) {
        ClientBadges.apply(new ClientUsers(false, List.of(player), List.of()));
    }
}
