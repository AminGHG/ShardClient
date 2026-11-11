package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.Notification;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Server notifications as cards stacked in the top-right corner. A card slides in from the right edge (where it
 * comes from), stays for a time that grows with its length, and slides back out; the cards below move up to
 * close the gap while it leaves. At most {@link #MAX_SHOWN} are shown; a new one pushes the oldest out.
 */
public final class NotificationToasts implements Overlay {

    private static final int WIDTH = 168;
    private static final int MARGIN = 8;
    private static final int PADDING = 8;
    private static final int GAP = 4;
    private static final int STRIPE = 2;
    private static final int MAX_SHOWN = 4;
    private static final long ENTER_MS = 220;
    private static final long BASE_MS = 4000;
    private static final long PER_CHAR_MS = 40;
    private static final long MAX_MS = 9000;
    /** How far a card travels in from the right, as a share of its width. */
    private static final float SLIDE = 0.4f;

    private final List<Card> cards = new ArrayList<>();
    private final TopCenter topCenter;

    /**
     * Where the top-centre cards (queue indicator, Match Found) are: their width and how far down they reach,
     * so notifications can step below them on screens too narrow to fit both side by side.
     */
    public interface TopCenter {
        int width();

        int bottom();
    }

    public NotificationToasts(TopCenter topCenter) {
        this.topCenter = topCenter;
    }

    private static final class Card {
        final Notification notification;
        final Transition transition = Motion.transition(ENTER_MS);
        final Tween top = new Tween(0, Motion.POPOVER_MS, Motion.MOVE);
        final long shownAt = Clock.SYSTEM.nanos();
        final long lifetimeMs;
        boolean placed;

        Card(Notification notification) {
            this.notification = notification;
            int length = notification.title().length() + notification.message().length();
            this.lifetimeMs = Math.min(MAX_MS, BASE_MS + PER_CHAR_MS * length);
            transition.show();
        }

        boolean expired() {
            return (Clock.SYSTEM.nanos() - shownAt) / 1_000_000L > lifetimeMs;
        }
    }

    public void push(Notification notification) {
        cards.add(0, new Card(notification));
        int shown = 0;
        for (Card card : cards) {
            if (card.transition.isShowing() && ++shown > MAX_SHOWN) {
                card.transition.hide();
            }
        }
    }

    public void clear() {
        cards.clear();
    }

    @Override
    public boolean render(Canvas canvas) {
        Palette colors = Theme.colors();
        int left = canvas.width() - MARGIN - WIDTH;
        int centerRight = (canvas.width() + topCenter.width()) / 2 + GAP;
        int y = topCenter.bottom() > 0 && left < centerRight ? topCenter.bottom() + GAP : MARGIN;
        for (Card card : List.copyOf(cards)) {
            if (card.transition.isShowing() && card.expired()) {
                card.transition.hide();
            }
            float progress = Math.clamp(card.transition.progress(), 0f, 1f);
            if (!card.transition.isVisible()) {
                cards.remove(card);
                continue;
            }
            Component title = Theme.text(Component.literal(title(card.notification)), true);
            List<FormattedCharSequence> lines =
                    canvas.wrap(Theme.text(Component.literal(card.notification.message()), false),
                            WIDTH - PADDING * 2 - STRIPE - 4);
            int lineHeight = canvas.lineHeight() + 1;
            int height = PADDING * 2 + lineHeight + lines.size() * lineHeight;

            if (!card.placed) {
                card.top.snap(y);
                card.placed = true;
            } else {
                card.top.set(y);
            }
            int top = Math.round(card.top.get());
            int x = canvas.width() - MARGIN - WIDTH + Math.round((1 - progress) * WIDTH * SLIDE);

            canvas.pushAlpha(progress);
            Theme.look().panel(canvas, x, top, WIDTH, height);
            int stripeColor = levelColor(card.notification.level(), colors);
            canvas.smoothRect(x + PADDING, top + PADDING, STRIPE, height - PADDING * 2, STRIPE / 2f, stripeColor);
            int textX = x + PADDING + STRIPE + 4;
            canvas.text(title, textX, top + PADDING, colors.text(), Theme.look().textShadow());
            int lineY = top + PADDING + lineHeight;
            for (FormattedCharSequence line : lines) {
                canvas.line(line, textX, lineY, colors.textMuted(), Theme.look().textShadow());
                lineY += lineHeight;
            }
            canvas.popAlpha();

            // A leaving card gives its space back as it goes, so the ones below glide up instead of jumping.
            y += Math.round((height + GAP) * progress);
        }
        return !cards.isEmpty();
    }

    private static String title(Notification notification) {
        if (!notification.title().isBlank()) {
            return notification.title();
        }
        return switch (notification.category()) {
            case MATCHMAKING -> "Matchmaking";
            case QUEUE -> "Queue";
            case ANNOUNCEMENT -> "Announcement";
            case PLAYER -> "Player";
            case GENERAL -> "ShardPvP";
        };
    }

    private static int levelColor(Notification.Level level, Palette colors) {
        return switch (level) {
            case INFO -> colors.accentStrong();
            case SUCCESS -> colors.positive();
            case WARNING -> colors.warning();
            case ERROR -> colors.negative();
        };
    }
}
