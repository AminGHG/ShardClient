package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.ui.PracticeOverlays;
import net.minecraft.resources.Identifier;

/**
 * The Practice Server integration, identical in the Full Client and the Light Version: the link to the server,
 * what the server has told this client, and the overlays that show it.
 */
public final class PracticeIntegration {

    private final PracticeSession session = new PracticeSession();
    private final PracticeLink link;
    private final OverlayHost overlays = new OverlayHost();

    public PracticeIntegration(ClientKind kind, String clientVersion) {
        this.link = new PracticeLink(kind, clientVersion, session);
        session.addListener(new PracticeLog());
        session.addListener(new PracticeOverlays(overlays));
    }

    /** Hooks into the game's networking and HUD. Call once, from the mod initializer. */
    public void register() {
        link.register();
        overlays.register(Identifier.fromNamespaceAndPath("shardclient-shared", "practice"));
    }

    public PracticeLink link() {
        return link;
    }

    public PracticeSession session() {
        return session;
    }

    public OverlayHost overlays() {
        return overlays;
    }
}
