package net.am7n.shardclient.protocol;

import net.am7n.shardclient.protocol.packet.Hello;
import net.am7n.shardclient.protocol.packet.Rejected;
import net.am7n.shardclient.protocol.packet.Welcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandshakeTest {

    private static final long SERVER_FEATURES = Feature.all();

    private static Hello hello(int min, int max) {
        return new Hello(min, max, ClientKind.FULL, "0.1.0", Feature.all());
    }

    @Test
    void aCurrentClientIsWelcomed() {
        assertEquals(new Welcome(Protocol.VERSION_MAX, Feature.all()),
                Handshake.answer(hello(Protocol.VERSION_MIN, Protocol.VERSION_MAX), true, SERVER_FEATURES));
    }

    @Test
    void aNewerClientThatStillSpeaksOurVersionIsWelcomedAtOurs() {
        assertEquals(new Welcome(Protocol.VERSION_MAX, Feature.all()),
                Handshake.answer(hello(Protocol.VERSION_MAX, Protocol.VERSION_MAX + 5), true, SERVER_FEATURES));
    }

    @Test
    void aClientWithNoVersionInCommonIsRejected() {
        assertEquals(new Rejected(Rejected.Reason.UNSUPPORTED_VERSION, Protocol.VERSION_MIN, Protocol.VERSION_MAX),
                Handshake.answer(hello(Protocol.VERSION_MAX + 1, Protocol.VERSION_MAX + 2), true, SERVER_FEATURES));
    }

    @Test
    void onlySharedFeaturesAreAgreed() {
        Hello notificationsOnly = new Hello(1, 1, ClientKind.LIGHT, "0.1.0",
                Feature.NOTIFICATIONS.mask() | 1L << 40);
        assertEquals(new Welcome(1, Feature.NOTIFICATIONS.mask()),
                Handshake.answer(notificationsOnly, true, SERVER_FEATURES));
    }

    @Test
    void nonsenseIsRejectedAsInvalid() {
        Rejected invalid = new Rejected(Rejected.Reason.INVALID_HELLO, Protocol.VERSION_MIN, Protocol.VERSION_MAX);
        assertEquals(invalid, Handshake.answer(hello(0, 1), true, SERVER_FEATURES));
        assertEquals(invalid, Handshake.answer(hello(-3, -1), true, SERVER_FEATURES));
        assertEquals(invalid, Handshake.answer(hello(2, 1), true, SERVER_FEATURES));
        assertEquals(invalid, Handshake.answer(new Hello(1, 1, 99, "0.1.0", 0), true, SERVER_FEATURES));
    }

    @Test
    void aSwitchedOffServerRejectsEveryone() {
        assertEquals(new Rejected(Rejected.Reason.DISABLED, Protocol.VERSION_MIN, Protocol.VERSION_MAX),
                Handshake.answer(hello(1, 1), false, SERVER_FEATURES));
    }

    @Test
    void negotiatePicksTheHighestCommonVersion() {
        assertEquals(Protocol.VERSION_MAX, Protocol.negotiate(1, 99));
        assertEquals(0, Protocol.negotiate(Protocol.VERSION_MAX + 1, 99));
        assertEquals(0, Protocol.negotiate(0, 0));
        assertEquals(0, Protocol.negotiate(5, 1));
    }
}
