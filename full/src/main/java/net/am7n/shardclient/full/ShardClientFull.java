package net.am7n.shardclient.full;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.api.ClientModInitializer;

/** Entrypoint of the Full Client. */
public final class ShardClientFull implements ClientModInitializer {

    public static final String MOD_ID = "shardclient";

    @Override
    public void onInitializeClient() {
        ShardClientRuntime.start(ClientKind.FULL, MOD_ID);
    }
}
