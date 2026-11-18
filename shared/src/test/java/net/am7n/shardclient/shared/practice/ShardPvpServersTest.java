package net.am7n.shardclient.shared.practice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardPvpServersTest {
    @Test
    void knowsTheShardPvpAddresses() {
        assertTrue(ShardPvpServers.matches("shardpvp.xyz"));
        assertTrue(ShardPvpServers.matches("ShardPvP.xyz:25565"));
        assertTrue(ShardPvpServers.matches("play.shardpvp.net"));
        assertTrue(ShardPvpServers.matches("mcprac.de."));
    }

    @Test
    void leavesOtherServersAlone() {
        assertFalse(ShardPvpServers.matches("donutsmp.net"));
        assertFalse(ShardPvpServers.matches("notshardpvp.xyz"));
        assertFalse(ShardPvpServers.matches("shardpvp.xyz.evil.com"));
        assertFalse(ShardPvpServers.matches(""));
        assertFalse(ShardPvpServers.matches(null));
    }
}
