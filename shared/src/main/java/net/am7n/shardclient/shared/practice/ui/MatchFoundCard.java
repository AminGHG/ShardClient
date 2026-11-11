package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Stagger;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.hud.Overlay;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.PlayerFace;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Match found": both players' faces and names, the gamemode, arena and the opponent's rank, near the top of the
 * screen. It pops in from 0.95, its parts settle one after another, it holds briefly and leaves. It never covers
 * the middle of the screen and takes no input, because the fight may already be on.
 */
public final class MatchFoundCard implements Overlay {

    private static final int WIDTH = 236;
    private static final int HEIGHT = 84;
    private static final int TOP = 24;
    private static final int FACE = 28;
    private static final int PADDING = 12;
    private static final long HOLD_MS = 3400;
    private static final long PART_MS = 240;
    /** How far each part rises while it settles. */
    private static final int SETTLE = 4;

    private final MatchInfo match;
    private final UUID selfId;
    private final String selfName;
    private final Transition transition = Motion.transition(Motion.POPOVER_MS);
    private final Stagger parts = new Stagger(5, Motion.ROW_STAGGER_MS * 2, PART_MS);
    private final long shownAt = Clock.SYSTEM.nanos();
    private String status = "";

    public static final int BOTTOM = TOP + HEIGHT;
    public static final int CARD_WIDTH = WIDTH;

    public boolean isShown() {
        return transition.isShowing();
    }

    public MatchFoundCard(MatchInfo match, UUID selfId, String selfName) {
        this.match = match;
        this.selfId = selfId;
        this.selfName = selfName;
        transition.show();
    }

    /** The server is moving the player into {@code arena}. */
    public void enteringArena(String arena) {
        status = arena.isEmpty() ? "Entering the arena" : "Entering " + arena;
    }

    public void dismiss() {
        transition.hide();
    }

    @Override
    public boolean render(Canvas canvas) {
        long elapsedMs = (Clock.SYSTEM.nanos() - shownAt) / 1_000_000L;
        if (elapsedMs > HOLD_MS) {
            transition.hide();
        }
        float progress = transition.progress();
        if (!transition.isVisible()) {
            return false;
        }
        Look look = Theme.look();
        Palette colors = look.colors();
        boolean shadow = look.textShadow();
        int x = (canvas.width() - WIDTH) / 2;
        int y = TOP;
        float linear = Math.clamp(elapsedMs / (float) parts.totalMs(), 0f, 1f);

        canvas.push();
        canvas.scale(Lerp.of(Motion.POPOVER_START_SCALE, 1f, progress), x + WIDTH / 2f, y + HEIGHT / 2f);
        canvas.pushAlpha(Math.clamp(progress, 0f, 1f));
        look.panel(canvas, x, y, WIDTH, HEIGHT);

        // 0: heading
        part(canvas, linear, 0, () -> canvas.textCentered(Theme.text(Component.literal("MATCH FOUND"), true),
                x + WIDTH / 2, y + PADDING, colors.accentStrong(), shadow));

        int faceY = y + PADDING + 14;
        int nameY = faceY + (FACE - 8) / 2;
        // 1: this player, left
        part(canvas, linear, 1, () -> {
            canvas.face(PlayerFace.skinOf(selfId), x + PADDING, faceY, FACE);
            canvas.text(Theme.text(Component.literal(selfName), true), x + PADDING + FACE + 6, nameY,
                    colors.text(), shadow);
        });
        // 2: VS
        part(canvas, linear, 2, () -> canvas.textCentered(Theme.text(Component.literal("VS"), true),
                x + WIDTH / 2, nameY, colors.textMuted(), shadow));
        // 3: the opponent, right
        part(canvas, linear, 3, () -> {
            int faceX = x + WIDTH - PADDING - FACE;
            canvas.face(PlayerFace.skinOf(match.opponentId()), faceX, faceY, FACE);
            canvas.textRight(Theme.text(Component.literal(match.opponentName()), true), faceX - 6, nameY,
                    colors.text(), shadow);
        });
        // 4: details
        part(canvas, linear, 4, () -> canvas.textCentered(Theme.text(Component.literal(details()), false),
                x + WIDTH / 2, y + HEIGHT - PADDING - 7, colors.textMuted(), shadow));

        canvas.popAlpha();
        canvas.pop();
        return true;
    }

    /** Draws part {@code index} faded and lifted by how far it has settled. */
    private void part(Canvas canvas, float linear, int index, Runnable draw) {
        float amount = parts.item(linear, index, Easing.OUT_CUBIC);
        if (amount <= 0) {
            return;
        }
        canvas.push();
        canvas.translate(0, (1 - amount) * SETTLE);
        canvas.pushAlpha(amount);
        draw.run();
        canvas.popAlpha();
        canvas.pop();
    }

    private String details() {
        List<String> bits = new ArrayList<>();
        bits.add(match.ranked() ? match.mode() + " Ranked" : match.mode());
        if (!match.kit().isEmpty()) {
            bits.add(match.kit());
        }
        if (!status.isEmpty()) {
            bits.add(status);
        } else if (!match.arena().isEmpty()) {
            bits.add(match.arena());
        }
        if (!match.opponentRank().isEmpty()) {
            bits.add(match.opponentRank());
        }
        return String.join("  ·  ", bits);
    }
}
