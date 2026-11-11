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
import net.am7n.shardclient.shared.net.ShardPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Detects a Practice Server and performs the handshake with it.
 *
 * <p>A server is a Practice Server when it announces the {@code shardclient:main} channel. Only then does this
 * client say hello, and only after the server's welcome are its packets acted on. On every other server, and in
 * singleplayer, nothing is ever sent and the link stays {@link State#INACTIVE}.
 */
public final class PracticeLink {

    public enum State {
        /** Not on a Practice Server (or not connected at all). */
        INACTIVE,
        /** The server announced the channel and was sent a hello; no answer yet. */
        HELLO_SENT,
        /** The server welcomed this client: the integration is live. */
        ACTIVE,
        /** The server refused this client; it behaves like plain Minecraft there. */
        REJECTED
    }

    /** What this build can display. Only announce a feature once the client really handles it. */
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

    /** Hooks into Fabric's networking. Call once, from the mod initializer. */
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(ShardPayload.TYPE, ShardPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShardPayload.TYPE, ShardPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(ShardPayload.TYPE,
                (payload, context) -> context.client().execute(() -> receive(payload.data())));

        // A new play connection (join, or a proxy moving the player to another server) starts from nothing.
        ClientPlayConnectionEvents.INIT.register((handler, client) -> client.execute(this::reset));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::reset));

        // The server's channel list can arrive before or after the join; whichever comes last finds it.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(this::sayHello));
        ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ShardPayload.TYPE.id())) {
                client.execute(this::sayHello);
            }
        });
    }

    public State state() {
        return state;
    }

    /** True while the Practice Server integration is live. */
    public boolean isActive() {
        return state == State.ACTIVE;
    }

    /** The protocol version agreed with the server; 0 unless {@link #isActive()}. */
    public int version() {
        return version;
    }

    /** Whether the server will send packets of {@code feature} on this connection. */
    public boolean has(Feature feature) {
        return isActive() && feature.in(features);
    }

    /** Why the server refused this client; null unless {@link State#REJECTED}. */
    public Rejected.Reason rejection() {
        return rejection;
    }

    private void sayHello() {
        if (state != State.INACTIVE) {
            return;
        }
        try {
            if (!ClientPlayNetworking.canSend(ShardPayload.TYPE)) {
                return;
            }
            Hello hello = new Hello(Protocol.VERSION_MIN, Protocol.VERSION_MAX, kind, clientVersion, FEATURES);
            ClientPlayNetworking.send(new ShardPayload(hello.encode()));
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
        switch (packet) {
            case null -> {
                // A packet from a newer server that this build does not know.
            }
            case Welcome welcome -> welcomed(welcome);
            case Rejected rejected -> rejected(rejected.reason());
            default -> {
                if (isActive() && packet.feature() != null && packet.feature().in(features)) {
                    session.handle(packet);
                }
            }
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
