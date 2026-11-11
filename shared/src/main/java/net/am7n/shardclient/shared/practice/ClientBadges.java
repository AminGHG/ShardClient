package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.ClientUsers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players on the current server run ShardClient (as the server says), and the badge drawn in front of
 * their names in the tab list and above their heads. Empty on any other server.
 */
public final class ClientBadges {

    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final FontDescription BADGE_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "badge"));

    private ClientBadges() {
    }

    static void apply(ClientUsers update) {
        if (update.replace()) {
            USERS.clear();
        }
        USERS.addAll(update.added());
        update.removed().forEach(USERS::remove);
    }

    static void clear() {
        USERS.clear();
    }

    public static boolean has(UUID player) {
        return player != null && USERS.contains(player);
    }

    /** {@code name} with the badge in front of it. */
    public static Component decorate(Component name) {
        MutableComponent badge = Component.literal("").withStyle(style -> style.withFont(BADGE_FONT));
        return Component.empty().append(badge).append(" ").append(name);
    }
}
