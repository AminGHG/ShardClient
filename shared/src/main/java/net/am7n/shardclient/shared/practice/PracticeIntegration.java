package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.ClientKind;

/**
 * The Practice Server integration, identical in the Full Client and the Light Version: the link to the server
 * and what the server has told this client.
 */
public final class PracticeIntegration {

    private final PracticeSession session = new PracticeSession();
    private final PracticeLink link;

    public PracticeIntegration(ClientKind kind, String clientVersion) {
        this.link = new PracticeLink(kind, clientVersion, session);
        session.addListener(new PracticeLog());
    }

    /** Hooks into the game's networking. Call once, from the mod initializer. */
    public void register() {
        link.register();
    }

    public PracticeLink link() {
        return link;
    }

    public PracticeSession session() {
        return session;
    }
}
