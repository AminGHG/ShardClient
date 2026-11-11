package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.resources.Identifier;

/**
 * Covers the screen with the ShardPvP logo while the server moves the player, then reveals the new place through
 * an opening from the centre. Started by the server's {@code TransitionStart}, lifted by the event that says the
 * move is done. It is only ever used where the player cannot be harmed during it, and it lifts by itself after
 * {@link #MAX_COVER_MS} so it can never trap anyone.
 */
public final class ScreenTransition implements Overlay {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("shardclient-shared", "textures/gui/logo.png");
    private static final int LOGO_TEXTURE = 256;
    private static final int LOGO_SIZE = 56;
    private static final long COVER_MS = 160;
    /** Exits are 20% shorter than entrances, but a reveal reads as the main motion, so it gets its own time. */
    private static final long REVEAL_MS = 380;
    /** The cover stays up at least this long, so it never flickers. */
    private static final long MIN_COVER_MS = 220;
    private static final long MAX_COVER_MS = 1500;

    private enum Phase { COVERING, REVEALING, DONE }

    private Phase phase = Phase.DONE;
    private long startedAt;
    private long revealAt;
    private boolean revealRequested;

    /** Covers the screen (again, if a reveal is still running). */
    public void cover() {
        long now = Clock.SYSTEM.nanos();
        if (phase == Phase.REVEALING) {
            // Turn around from the current state instead of jumping to black.
            float shown = 1 - revealProgress(now);
            startedAt = now - (long) (shown * COVER_MS * 1_000_000L);
        } else if (phase == Phase.DONE) {
            startedAt = now;
        }
        phase = Phase.COVERING;
        revealRequested = false;
    }

    /** The move is done: reveal as soon as the cover has been up for its minimum time. */
    public void reveal() {
        if (phase == Phase.COVERING) {
            revealRequested = true;
        }
    }

    public boolean isActive() {
        return phase != Phase.DONE;
    }

    public void reset() {
        phase = Phase.DONE;
    }

    @Override
    public boolean render(Canvas canvas) {
        long now = Clock.SYSTEM.nanos();
        float cover;
        float opening = 0;
        if (phase == Phase.COVERING) {
            long elapsedMs = (now - startedAt) / 1_000_000L;
            if ((revealRequested && elapsedMs >= MIN_COVER_MS) || elapsedMs >= MAX_COVER_MS) {
                phase = Phase.REVEALING;
                revealAt = now;
            }
            cover = Motion.ENTER.apply(Math.clamp(elapsedMs / (float) COVER_MS, 0f, 1f));
        } else if (phase == Phase.REVEALING) {
            float t = revealProgress(now);
            if (t >= 1) {
                phase = Phase.DONE;
                return false;
            }
            opening = Easing.IN_OUT_CUBIC.apply(t);
            cover = 1 - Easing.IN_CUBIC.apply(t);
        } else {
            return false;
        }
        drawCover(canvas, cover, opening);
        return true;
    }

    private float revealProgress(long now) {
        return Math.clamp((now - revealAt) / (REVEAL_MS * 1_000_000f), 0f, 1f);
    }

    /**
     * The cover with a centred opening of {@code opening} (0 closed, 1 the whole screen): the new place shows
     * through a mask that widens from the middle while the rest fades.
     */
    private void drawCover(Canvas canvas, float alpha, float opening) {
        int width = canvas.width();
        int height = canvas.height();
        int holeWidth = Math.round(width * opening);
        int holeHeight = Math.round(height * opening);
        int left = (width - holeWidth) / 2;
        int top = (height - holeHeight) / 2;
        int color = Theme.colors().bg0();

        canvas.pushAlpha(Math.clamp(alpha, 0f, 1f));
        if (holeWidth <= 0 || holeHeight <= 0) {
            canvas.fill(0, 0, width, height, color);
        } else {
            canvas.fill(0, 0, width, top, color);
            canvas.fill(0, top + holeHeight, width, height - top - holeHeight, color);
            canvas.fill(0, top, left, holeHeight, color);
            canvas.fill(left + holeWidth, top, width - left - holeWidth, holeHeight, color);
        }
        float logoScale = Lerp.of(Motion.POPOVER_START_SCALE, 1f, Math.clamp(alpha, 0f, 1f));
        int size = Math.round(LOGO_SIZE * logoScale);
        canvas.pushAlpha(1 - opening);
        canvas.image(LOGO, LOGO_TEXTURE, (width - size) / 2, (height - size) / 2, size);
        canvas.popAlpha();
        canvas.popAlpha();
    }
}
