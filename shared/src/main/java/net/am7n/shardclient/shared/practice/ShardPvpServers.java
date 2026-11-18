package net.am7n.shardclient.shared.practice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.List;
import java.util.Locale;

public final class ShardPvpServers {
    private static final List<String> DOMAINS = List.of("shardpvp.xyz", "shardpvp.net", "shardsmp.net",
            "centralpvp.de", "mcprac.de", "lushsmp.net");

    private ShardPvpServers() {
    }

    public static boolean isCurrent(Minecraft client) {
        ServerData server = client.getCurrentServer();
        return server != null && matches(server.ip);
    }

    static boolean matches(String address) {
        if (address == null) {
            return false;
        }
        String host = address.trim().toLowerCase(Locale.ROOT);
        int port = host.lastIndexOf(':');
        if (port > 0 && host.indexOf(':') == port) {
            host = host.substring(0, port);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        for (String domain : DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }
}
