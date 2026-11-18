package net.am7n.shardclient.shared.practice;

import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.ui.PracticeOverlays;

public final class PracticeIntegration {
    private final PracticeSession session = new PracticeSession();
    private final PracticeLink link;
    private final OverlayHost overlays = new OverlayHost();

    public PracticeIntegration(ClientKind kind, String clientVersion) {
        this.link = new PracticeLink(kind, clientVersion, session);
        session.addListener(new PracticeLog());
        session.addListener(new PracticeOverlays(overlays));
    }

    public void register() {
        link.register();
        overlays.register("practice");
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
