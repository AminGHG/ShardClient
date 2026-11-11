package net.am7n.shardclient.shared;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.practice.PracticeIntegration;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What is running: set once by the product's initializer, read by everything in the shared library that needs to
 * know which product it lives in (the handshake reports it to the server).
 */
public final class ShardClientRuntime {

    public static final Logger LOGGER = LoggerFactory.getLogger("ShardClient");

    private static volatile ShardClientRuntime instance;

    private final ClientKind kind;
    private final String modId;
    private final String version;
    private final PracticeIntegration practice;

    private ShardClientRuntime(ClientKind kind, String modId, String version) {
        this.kind = kind;
        this.modId = modId;
        this.version = version;
        this.practice = new PracticeIntegration(kind, version);
    }

    /** Called by the Full Client or the Light Version from its client entrypoint, exactly once. */
    public static synchronized ShardClientRuntime start(ClientKind kind, String modId) {
        if (instance != null) {
            throw new IllegalStateException("ShardClient already started as " + instance.kind
                    + " (" + instance.modId + "); only one product may be installed");
        }
        String version = FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElseThrow(() -> new IllegalStateException("Mod " + modId + " is not loaded"));
        ShardClientRuntime runtime = new ShardClientRuntime(kind, modId, version);
        runtime.practice.register();
        instance = runtime;
        LOGGER.info("ShardClient {} {} started", kind, version);
        return runtime;
    }

    public static ShardClientRuntime get() {
        ShardClientRuntime current = instance;
        if (current == null) {
            throw new IllegalStateException("ShardClient has not been started");
        }
        return current;
    }

    public ClientKind kind() {
        return kind;
    }

    public String modId() {
        return modId;
    }

    public String version() {
        return version;
    }

    /** The Practice Server integration; present in both products. */
    public PracticeIntegration practice() {
        return practice;
    }
}
