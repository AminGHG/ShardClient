package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.packet.ArenaTransition;
import net.am7n.shardclient.protocol.packet.MatchFound;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.protocol.packet.QueueJoined;
import net.am7n.shardclient.protocol.packet.QueueLeft;
import net.am7n.shardclient.protocol.packet.QueueUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PracticeSessionTest {

    private static final UUID OPPONENT = UUID.fromString("0f6f2f0e-7a51-4c1e-9d0b-1f0c2b3a4d5e");

    private final List<String> events = new ArrayList<>();
    private PracticeSession session;

    @BeforeEach
    void setUp() {
        session = new PracticeSession();
        session.addListener(new PracticeListener() {
            @Override
            public void onQueueJoined(QueueState queue) {
                events.add("joined " + queue.id());
            }

            @Override
            public void onQueueUpdated(QueueState queue) {
                events.add("update " + queue.waitSeconds() + "s " + queue.playersQueued());
            }

            @Override
            public void onQueueLeft(QueueState queue, QueueLeft.Reason reason) {
                events.add("left " + queue.id() + " " + reason);
            }

            @Override
            public void onMatchFound(MatchInfo match) {
                events.add("match " + match.opponentName());
            }

            @Override
            public void onArenaTransition(String arena) {
                events.add("arena " + arena);
            }

            @Override
            public void onNotification(Notification notification) {
                events.add("note " + notification.message());
            }
        });
    }

    @Test
    void queueLifecycleFollowsTheServer() {
        session.handle(new QueueJoined("1v1", "1v1", false));
        session.handle(new QueueUpdate("1v1", 3, 2));
        assertEquals(new QueueState("1v1", "1v1", false, 3, 2), session.queue());

        session.handle(new QueueLeft("1v1", QueueLeft.Reason.LEFT));
        assertNull(session.queue());
        assertEquals(List.of("joined 1v1", "update 3s 2", "left 1v1 LEFT"), events);
    }

    @Test
    void aMatchEndsTheQueueWithoutALeftEvent() {
        session.handle(new QueueJoined("1v1", "1v1", false));
        session.handle(new MatchFound(OPPONENT, "Rival", "1v1", "", "badlands", false, ""));
        session.handle(new ArenaTransition("badlands"));

        assertNull(session.queue());
        assertNotNull(session.match());
        assertEquals("Rival", session.match().opponentName());
        assertEquals(List.of("joined 1v1", "match Rival", "arena badlands"), events);
    }

    @Test
    void updatesAndLeavesForAQueueThePlayerIsNotInAreIgnored() {
        session.handle(new QueueUpdate("1v1", 3, 2));
        session.handle(new QueueLeft("1v1", QueueLeft.Reason.CANCELLED));
        session.handle(new QueueJoined("1v1", "1v1", false));
        session.handle(new QueueUpdate("ranked", 9, 9));

        assertEquals(new QueueState("1v1", "1v1", false, 0, 0), session.queue());
        assertEquals(List.of("joined 1v1"), events);
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        PracticeSession noisy = new PracticeSession();
        noisy.addListener(new PracticeListener() {
            @Override
            public void onNotification(Notification notification) {
                throw new IllegalStateException("broken listener");
            }
        });
        List<String> seen = new ArrayList<>();
        noisy.addListener(new PracticeListener() {
            @Override
            public void onNotification(Notification notification) {
                seen.add(notification.message());
            }
        });

        noisy.handle(new Notification(Notification.Category.GENERAL, Notification.Level.INFO, "", "hello"));
        assertEquals(List.of("hello"), seen);
    }

    @Test
    void clearingForgetsEverything() {
        session.handle(new QueueJoined("1v1", "1v1", false));
        session.handle(new MatchFound(OPPONENT, "Rival", "1v1", "", "", false, ""));
        session.clear();
        assertNull(session.queue());
        assertNull(session.match());
    }
}
