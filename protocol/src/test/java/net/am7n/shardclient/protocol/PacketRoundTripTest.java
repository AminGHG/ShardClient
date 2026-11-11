package net.am7n.shardclient.protocol;

import net.am7n.shardclient.protocol.packet.ArenaTransition;
import net.am7n.shardclient.protocol.packet.ClientboundPacket;
import net.am7n.shardclient.protocol.packet.Hello;
import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.protocol.packet.MatchFound;
import net.am7n.shardclient.protocol.packet.MatchStarted;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueJoined;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.protocol.packet.QueueUpdate;
import net.am7n.shardclient.protocol.packet.Rejected;
import net.am7n.shardclient.protocol.packet.ReturnToLobby;
import net.am7n.shardclient.protocol.packet.RoundEnded;
import net.am7n.shardclient.protocol.packet.ServerboundPacket;
import net.am7n.shardclient.protocol.packet.TransitionStart;
import net.am7n.shardclient.protocol.packet.Welcome;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketRoundTripTest {

    private static final List<ClientboundPacket> CLIENTBOUND = List.of(
            new Welcome(1, Feature.all()),
            new Rejected(Rejected.Reason.UNSUPPORTED_VERSION, 1, 3),
            new QueueJoined("1v1", "1v1", false),
            new QueueLeft("1v1", QueueLeft.Reason.CANCELLED),
            new QueueUpdate("1v1", 73, 4),
            new MatchFound(UUID.fromString("0f6f2f0e-7a51-4c1e-9d0b-1f0c2b3a4d5e"), "Opponent_16chars",
                    "1v1", "", "badlands", false, ""),
            new ArenaTransition("badlands"),
            new Notification(Notification.Category.PLAYER, Notification.Level.WARNING, "Duel", "ä ö ü → 😀"),
            new TransitionStart(TransitionStart.Kind.ARENA),
            new MatchStarted(2, 2, 1, 0),
            new RoundEnded(true, 2, 1),
            new MatchEnded(MatchEnded.Result.WIN, 2, 1, 1172, 1214, "Gold I", 1100, 1200, "Gold II", 1200, 1300),
            new ReturnToLobby());

    @Test
    void everyClientboundPacketSurvivesTheWire() throws ProtocolException {
        for (ClientboundPacket packet : CLIENTBOUND) {
            assertEquals(packet, ClientboundPacket.decode(packet.encode()), packet.getClass().getSimpleName());
        }
    }

    @Test
    void clientboundIdsAreUnique() {
        Set<Integer> ids = new HashSet<>();
        for (ClientboundPacket packet : CLIENTBOUND) {
            assertTrue(ids.add(packet.id()), "duplicate id " + packet.id());
        }
    }

    @Test
    void helloSurvivesTheWire() throws ProtocolException {
        Hello hello = new Hello(1, 1, ClientKind.LIGHT, "0.1.0", Feature.all());
        assertEquals(hello, ServerboundPacket.decode(hello.encode()));
    }

    @Test
    void unknownPacketIdsAreIgnored() throws ProtocolException {
        assertNull(ClientboundPacket.decode(new byte[]{(byte) 0xEE, 1, 2, 3}));
        assertNull(ServerboundPacket.decode(new byte[]{(byte) 0xEE}));
    }

    @Test
    void bytesAfterTheKnownFieldsAreIgnored() throws ProtocolException {
        QueueUpdate packet = new QueueUpdate("1v1", 5, 2);
        byte[] wire = packet.encode();
        byte[] extended = Arrays.copyOf(wire, wire.length + 9);
        assertEquals(packet, ClientboundPacket.decode(extended));
    }

    @Test
    void everyTruncationIsAnErrorNotACrash() {
        for (ClientboundPacket packet : CLIENTBOUND) {
            byte[] wire = packet.encode();
            for (int length = 0; length < wire.length; length++) {
                byte[] cut = Arrays.copyOf(wire, length);
                assertThrows(ProtocolException.class, () -> ClientboundPacket.decode(cut),
                        packet.getClass().getSimpleName() + " cut to " + length);
            }
        }
        assertThrows(ProtocolException.class, () -> ServerboundPacket.decode(new byte[0]));
    }

    @Test
    void oversizedMessagesAreRefusedUnread() {
        assertThrows(ProtocolException.class,
                () -> ServerboundPacket.decode(new byte[Protocol.MAX_SERVERBOUND_BYTES + 1]));
        assertThrows(ProtocolException.class,
                () -> ClientboundPacket.decode(new byte[Protocol.MAX_CLIENTBOUND_BYTES + 1]));
    }

    @Test
    void aStringLongerThanItsFieldAllowsIsAnError() {
        // A hello whose version string claims 200 bytes where 32 chars are allowed.
        byte[] wire = new PacketWriter().writeByte(Hello.ID).writeInt(1).writeInt(1).writeByte(1)
                .writeString("x".repeat(200), 200).writeLong(0).toByteArray();
        assertThrows(ProtocolException.class, () -> ServerboundPacket.decode(wire));
    }

    @Test
    void writingCutsOverlongTextInsteadOfFailing() throws ProtocolException {
        Notification sent = new Notification(Notification.Category.GENERAL, Notification.Level.INFO,
                "t".repeat(500), "m".repeat(5000));
        Notification read = (Notification) ClientboundPacket.decode(sent.encode());
        assertEquals(Notification.MAX_TITLE_CHARS, read.title().length());
        assertEquals(Notification.MAX_MESSAGE_CHARS, read.message().length());
    }

    @Test
    void cuttingNeverSplitsASurrogatePair() {
        String emoji = "😀";
        assertEquals("ab", PacketWriter.truncate("ab" + emoji, 3));
        assertEquals("ab" + emoji, PacketWriter.truncate("ab" + emoji, 4));
    }

    @Test
    void unknownEnumValuesFallBack() throws ProtocolException {
        byte[] wire = new PacketWriter().writeByte(QueueLeft.ID).writeString("1v1", 32).writeByte(200).toByteArray();
        assertEquals(new QueueLeft("1v1", QueueLeft.Reason.OTHER), ClientboundPacket.decode(wire));
    }

    @Test
    void helloVersionIsMadeSafeForLogs() {
        Hello hello = new Hello(1, 1, ClientKind.FULL, "1.0\n[Server] fake", 0);
        assertEquals("1.0?[Server]?fake", hello.printableClientVersion());
    }
}
