package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.ClientUsers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientBadges {
    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final FontDescription BADGE_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "badge"));
    private static final String BADGE_GLYPH = "";
    private static final int REFRESH_TICKS = 10;

    private static volatile List<String> names = List.of();
    private static volatile int version;
    private static int ticks;

    private ClientBadges() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientBadges::tick);
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
        return player != null && (USERS.contains(player)
                || net.am7n.shardclient.shared.badges.GlobalBadges.has(player));
    }

    public static int version() {
        return version;
    }

    private static void tick(Minecraft client) {
        if (++ticks < REFRESH_TICKS) {
            return;
        }
        ticks = 0;
        List<String> next = new ArrayList<>();
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                String name = info.getProfile().name();
                if (name != null && !name.isEmpty() && has(info.getProfile().id())) {
                    next.add(name);
                }
            }
        }
        next.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
        if (!next.equals(names)) {
            names = List.copyOf(next);
            version++;
        }
    }

    public static Component decorate(Component display, String name) {
        if (display == null) {
            return null;
        }
        String flat = display.getString();
        int index = name == null || name.isEmpty() ? -1 : find(flat, name);
        return insert(display, index < 0 ? 0 : lineStart(flat, index));
    }

    public static Component decorateAny(Component text) {
        return decorateAny(text, names);
    }

    static Component decorateAny(Component text, List<String> current) {
        if (text == null || current.isEmpty()) {
            return text;
        }
        String flat = text.getString();
        int best = firstName(flat, current, flat.length());
        return best < 0 ? text : insert(text, lineStart(flat, best));
    }

    public static Component decorateChat(Component message) {
        return decorateChat(message, names);
    }

    static Component decorateChat(Component message, List<String> current) {
        if (message == null || current.isEmpty()) {
            return message;
        }
        String flat = message.getString();
        int sender = senderEnd(flat);
        if (sender < 0) {
            return message;
        }
        int best = firstName(flat, current, sender);
        return best < 0 ? message : insert(message, lineStart(flat, best));
    }

    private static int firstName(String flat, List<String> current, int before) {
        int best = -1;
        for (String name : current) {
            int index = find(flat, name);
            if (index >= 0 && index < before && (best < 0 || index < best)) {
                best = index;
            }
        }
        return best;
    }

    private static int senderEnd(String flat) {
        int end = -1;
        for (char separator : new char[]{':', '»', '>'}) {
            int index = flat.indexOf(separator);
            if (index >= 0 && (end < 0 || index < end)) {
                end = index;
            }
        }
        return end;
    }

    private static int lineStart(String flat, int index) {
        return flat.lastIndexOf('\n', index - 1) + 1;
    }

    private static Component badge() {
        return Component.literal(BADGE_GLYPH).withStyle(style -> style.withFont(BADGE_FONT));
    }

    static int find(String text, String name) {
        int from = 0;
        while (true) {
            int index = indexOfIgnoreCase(text, name, from);
            if (index < 0) {
                return -1;
            }
            int end = index + name.length();
            boolean startOk = index == 0 || !isNameChar(text.charAt(index - 1));
            boolean endOk = end == text.length() || !isNameChar(text.charAt(end));
            if (startOk && endOk) {
                return index;
            }
            from = index + 1;
        }
    }

    private static int indexOfIgnoreCase(String text, String part, int from) {
        for (int i = from; i + part.length() <= text.length(); i++) {
            if (text.regionMatches(true, i, part, 0, part.length())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static Component insert(Component text, int index) {
        if (index <= 0) {
            return Component.empty().append(badge()).append(" ").append(text);
        }
        MutableComponent out = Component.empty();
        int[] position = {0};
        boolean[] inserted = {false};
        text.visit((style, content) -> {
            int start = position[0];
            int end = start + content.length();
            if (!inserted[0] && index >= start && index < end) {
                int split = index - start;
                if (split > 0) {
                    out.append(Component.literal(content.substring(0, split)).withStyle(style));
                }
                out.append(badge());
                out.append(Component.literal(" ").withStyle(style));
                out.append(Component.literal(content.substring(split)).withStyle(style));
                inserted[0] = true;
            } else {
                out.append(Component.literal(content).withStyle(style));
            }
            position[0] = end;
            return Optional.empty();
        }, Style.EMPTY);
        if (!inserted[0]) {
            return Component.empty().append(badge()).append(" ").append(text);
        }
        return out;
    }
}
