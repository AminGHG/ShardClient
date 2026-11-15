package net.am7n.shardclient.light;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.api.ClientModInitializer;

public final class ShardClientLight implements ClientModInitializer {
    public static final String MOD_ID = "shardclient-light";

    @Override
    public void onInitializeClient() {
        ShardClientRuntime.start(ClientKind.LIGHT, MOD_ID);
    }
}
