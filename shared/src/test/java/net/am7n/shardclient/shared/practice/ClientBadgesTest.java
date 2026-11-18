package net.am7n.shardclient.shared.practice;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientBadgesTest {
    private static final String BADGE = " ";
    private static final List<String> USERS = List.of("yztv_");

    @Test
    void findsWholeNamesOnly() {
        assertEquals(6, ClientBadges.find("[VIP] Steve", "Steve"));
        assertEquals(-1, ClientBadges.find("[VIP] Steve_2", "Steve"));
        assertEquals(-1, ClientBadges.find("xSteve", "Steve"));
        assertEquals(7, ClientBadges.find("xSteve Steve", "Steve"));
        assertEquals(0, ClientBadges.find("steve: hi", "Steve"));
    }

    @Test
    void badgeGoesInFrontOfTheRankPrefix() {
        Component tag = Component.empty()
                .append(Component.literal("VIP ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("yztv_").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" | -4").withStyle(ChatFormatting.GREEN));
        assertEquals(BADGE + "VIP yztv_ | -4", ClientBadges.decorate(tag, "yztv_").getString());
    }

    @Test
    void multiLineNametagGetsTheBadgeOnTheNameLine() {
        Component tag = Component.literal("Level 12\nVIP yztv_ | -4\nLTS | #299");
        assertEquals("Level 12\n" + BADGE + "VIP yztv_ | -4\nLTS | #299",
                ClientBadges.decorateAny(tag, USERS).getString());
    }

    @Test
    void chatBadgeGoesBeforeTheSender() {
        Component chat = Component.literal("VIP yztv_: gg");
        assertEquals(BADGE + "VIP yztv_: gg", ClientBadges.decorateChat(chat, USERS).getString());
        Component vanilla = Component.literal("<yztv_> hi");
        assertEquals(BADGE + "<yztv_> hi", ClientBadges.decorateChat(vanilla, USERS).getString());
    }

    @Test
    void chatMentionsAndSystemMessagesStayUntouched() {
        Component mention = Component.literal("Steve: gg yztv_");
        assertSame(mention, ClientBadges.decorateChat(mention, USERS));
        Component join = Component.literal("yztv_ joined the game");
        assertSame(join, ClientBadges.decorateChat(join, USERS));
    }

    @Test
    void otherTextIsUntouchedWithoutBadgeUsers() {
        Component hologram = Component.literal("[VIP] Steve");
        assertSame(hologram, ClientBadges.decorateAny(hologram));
    }
}
