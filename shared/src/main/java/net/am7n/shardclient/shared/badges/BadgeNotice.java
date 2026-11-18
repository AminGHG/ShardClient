package net.am7n.shardclient.shared.badges;

import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.ui.NotificationToasts;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class BadgeNotice {
    private static final String TITLE = "ShardClient";

    private static final OverlayHost HOST = new OverlayHost();
    private static final NotificationToasts TOASTS = new NotificationToasts(new NotificationToasts.TopCenter() {
        @Override
        public int width() {
            return 0;
        }

        @Override
        public int bottom() {
            return 0;
        }
    });

    private BadgeNotice() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("shardclient-shared", "badge_notice"),
                (graphics, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.gui.screen() == null) {
                        HOST.render(new Canvas(graphics, mc.font, -1, -1));
                    }
                });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) ->
                        HOST.render(new Canvas(graphics, client.font, mouseX, mouseY))));
    }

    public static void warn(String message) {
        show(Notification.Level.WARNING, message);
    }

    public static void ok(String message) {
        show(Notification.Level.SUCCESS, message);
    }

    private static void show(Notification.Level level, String message) {
        Minecraft.getInstance().execute(() -> {
            TOASTS.push(new Notification(Notification.Category.GENERAL, level, TITLE, message));
            HOST.add(TOASTS);
        });
    }
}
