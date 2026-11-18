package net.am7n.shardclient.shared.badges;

import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.shared.platform.Hud;
import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.shared.hud.OverlayHost;
import net.am7n.shardclient.shared.practice.ui.NotificationToasts;
import net.minecraft.client.Minecraft;

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
        Hud.addLayer("badge_notice", canvas -> {
            if (Game.screen() == null) {
                HOST.render(canvas);
            }
        });
        Hud.afterEveryScreen(HOST::render);
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
