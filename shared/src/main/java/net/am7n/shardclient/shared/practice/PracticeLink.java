package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.protocol.ProtocolException;
import net.am7n.shardclient.protocol.packet.ClientboundPacket;
import net.am7n.shardclient.protocol.packet.Hello;
import net.am7n.shardclient.protocol.packet.Rejected;
import net.am7n.shardclient.protocol.packet.Welcome;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.platform.Channel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class PracticeLink {
    public enum State {
        INACTIVE,

        HELLO_SENT,

        ACTIVE,

        REJECTED
    }

    private static final long FEATURES = Feature.all();

    private final ClientKind kind;
    private final String clientVersion;
    private final PracticeSession session;

    private State state = State.INACTIVE;
    private int version;
    private long features;
    private Rejected.Reason rejection;

    public PracticeLink(ClientKind kind, String clientVersion, PracticeSession session) {
        this.kind = kind;
        this.clientVersion = clientVersion;
        this.session = session;
    }

    public void register() {
        Channel.register(this::receive);

        ClientPlayConnectionEvents.INIT.register((handler, client) -> client.execute(this::reset));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::reset));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            boolean offered = Channel.offered();
            ShardClientRuntime.LOGGER.info("Joined a server; ShardPvP channel offered: {}", offered);
            if (offered) {
                sayHello();
            } else if (ShardPvpServers.isCurrent(client)) {
                ShardClientRuntime.LOGGER.info("ShardPvP server did not offer the channel, saying hello anyway");
                sayHello(true);
            }
        }));
        Channel.onOffered(this::sayHello);
    }

    public State state() {
        return state;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public int version() {
        return version;
    }

    public boolean has(Feature feature) {
        return isActive() && feature.in(features);
    }

    public Rejected.Reason rejection() {
        return rejection;
    }

    private void sayHello() {
        sayHello(false);
    }

    private void sayHello(boolean evenIfNotOffered) {
        if (state != State.INACTIVE) {
            return;
        }
        try {
            if (!evenIfNotOffered && !Channel.offered()) {
                return;
            }
            Hello hello = new Hello(Protocol.VERSION_MIN, Protocol.VERSION_MAX, kind, clientVersion, FEATURES);
            Channel.send(hello.encode());
            ShardClientRuntime.LOGGER.info("Sent ShardPvP hello (protocol {}-{})", Protocol.VERSION_MIN,
                    Protocol.VERSION_MAX);
        } catch (IllegalStateException notConnected) {
            return;
        }
        change(State.HELLO_SENT);
    }

    private void receive(byte[] message) {
        if (state == State.INACTIVE) {
            return;
        }
        ClientboundPacket packet;
        try {
            packet = ClientboundPacket.decode(message);
        } catch (ProtocolException ex) {
            ShardClientRuntime.LOGGER.warn("Ignoring an unreadable Practice Server packet: {}", ex.getMessage());
            return;
        }
        if (packet == null) {
            return;
        }
        if (packet instanceof Welcome welcome) {
            welcomed(welcome);
        } else if (packet instanceof Rejected rejected) {
            rejected(rejected.reason());
        } else if (isActive() && packet.feature() != null && packet.feature().in(features)) {
            session.handle(packet);
        }
    }

    private void welcomed(Welcome welcome) {
        if (state != State.HELLO_SENT) {
            return;
        }
        if (welcome.version() < Protocol.VERSION_MIN || welcome.version() > Protocol.VERSION_MAX) {
            rejected(Rejected.Reason.UNSUPPORTED_VERSION);
            return;
        }
        version = welcome.version();
        features = welcome.features() & FEATURES;
        ShardClientRuntime.LOGGER.info("Practice Server integration active (protocol {})", version);
        change(State.ACTIVE);
    }

    private void rejected(Rejected.Reason reason) {
        session.clear();
        version = 0;
        features = 0;
        rejection = reason;
        ShardClientRuntime.LOGGER.info("Practice Server integration off: {}", reason);
        change(State.REJECTED);
    }

    private void reset() {
        session.clear();
        version = 0;
        features = 0;
        rejection = null;
        change(State.INACTIVE);
    }

    private void change(State next) {
        if (state != next) {
            state = next;
            session.linkChanged(next);
        }
    }
}
