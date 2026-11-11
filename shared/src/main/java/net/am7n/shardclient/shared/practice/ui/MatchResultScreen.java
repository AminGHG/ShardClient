package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.PlayerFace;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * The end of a ranked match as a full-screen sequence, shown in the lobby once the match is over: the result
 * rises out of a mask, then the players, the score, the rating counting to its new value and the rank settle one
 * after another. It can be skipped at any moment (button, Space or Esc), continues by itself after
 * {@link #AUTO_CLOSE_MS}, and never pauses the game.
 */
public final class MatchResultScreen extends ShardScreen {

    private static final long AUTO_CLOSE_MS = 9000;
    /** When the rank bar starts moving, once the parts above it have settled. */
    private static final long BAR_START_MS = 1250;

    private final MatchEnded result;
    private final MatchInfo match;
    private final UUID selfId;
    private final String selfName;
    private final long shownAt = Clock.SYSTEM.nanos();
    private final ResultEffects effects;
    /** Rating as progress through the rank, when the server sent the rank ranges. */
    private final RankProgressBar bar;

    /**
     * @param match the match as it was found (opponent); null if this client did not see it start
     */
    public MatchResultScreen(Screen parent, MatchEnded result, MatchInfo match, UUID selfId, String selfName) {
        super(Component.literal("Match result"), parent);
        this.result = result;
        this.match = match;
        this.selfId = selfId;
        this.selfName = selfName;
        this.bar = result.hasRankProgress() ? new RankProgressBar(result) : null;
        this.effects = new ResultEffects(result.result() == MatchEnded.Result.WIN);
    }

    private long elapsedMs() {
        return (Clock.SYSTEM.nanos() - shownAt) / 1_000_000L;
    }

    @Override
    protected Element build() {
        return new View();
    }

    @Override
    protected void arrange(Element root, int screenWidth, int screenHeight) {
        root.place(0, 0, screenWidth, screenHeight);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isClosing() && elapsedMs() > AUTO_CLOSE_MS) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 32) { // Space skips, like the button
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Everything on the screen; laid out from the screen size each frame. */
    private final class View extends Container {

        private static final int SKIP_MARGIN = 12;

        private final Button skip = new Button("Skip  (Space)", MatchResultScreen.this::onClose);

        View() {
            add(skip);
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            Size size = skip.measure();
            skip.place(x + width - SKIP_MARGIN - size.width(), y + height - SKIP_MARGIN - size.height(),
                    size.width(), size.height());
        }

        @Override
        public void render(Canvas canvas) {
            long t = elapsedMs();
            Look look = Theme.look();
            Palette colors = look.colors();
            int cx = x + width / 2;
            int top = Math.max(16, Math.round(height * 0.22f));

            effects.drift(canvas, colors, t, openProgress());
            heading(canvas, t, colors, cx, top);
            players(canvas, t, look, cx, top + 52);
            score(canvas, t, look, cx, top + 86);
            if (bar != null) {
                if (settle(canvas, t, BAR_START_MS - 200) > 0) {
                    bar.render(canvas, cx, top + 118, Math.min(260, width - 40), Math.max(0, t - BAR_START_MS));
                    endSettle(canvas);
                }
                if (result.rankChanged()) {
                    rank(canvas, t, look, cx, top + 160, BAR_START_MS + bar.durationMs());
                }
            } else {
                if (result.rated()) {
                    rating(canvas, t, look, cx, top + 120);
                }
                if (!result.rankAfter().isEmpty()) {
                    rank(canvas, t, look, cx, top + 150, 1300);
                }
            }
            autoContinue(canvas, t, colors);
            super.render(canvas);
        }

        /** The result word, rising out of a mask line, with an underline growing from the centre. */
        private void heading(Canvas canvas, long t, Palette colors, int cx, int top) {
            Component text = Theme.text(Component.literal(headingText()), true);
            int textWidth = Math.max(1, canvas.textWidth(text));
            float scale = Math.min(4f, (width - 40) / (float) textWidth);
            int lineHeight = Math.round(9 * scale);
            // Rendered at the size it is drawn, so the big headline stays sharp.
            Component sharp = Theme.text(Component.literal(headingText()), true, scale);
            float rise = Motion.ENTER.apply(window(t, 100, 520));

            int textPixels = Math.round(textWidth * scale);
            // A soft glow of the result colour behind the headline, growing in with it.
            canvas.glow(cx, top + lineHeight / 2, Math.round(textPixels * 0.75f), Math.round(lineHeight * 1.9f),
                    withAlpha(headingColor(colors), victory() ? 0x5C : 0x3A), rise);

            java.util.function.IntConsumer draw = color -> {
                canvas.push();
                canvas.translate(0, (1 - rise) * lineHeight);
                canvas.scale(scale, cx, top);
                canvas.textCentered(sharp, cx, top, color, Theme.look().textShadow());
                canvas.pop();
            };
            canvas.clip(0, top, width, lineHeight + 2);
            draw.accept(headingColor(colors));
            canvas.unclip();
            // Once it has landed, a light sheen crosses it.
            effects.sheen(canvas, window(t, 760, 700), cx - textPixels / 2, top, textPixels, lineHeight + 2,
                    colors.text(), draw);
            effects.burst(canvas, colors, t - 560, cx, top + lineHeight / 2);

            float grow = Motion.ENTER.apply(window(t, 420, 420));
            int lineWidth = Math.round(textWidth * scale * 0.5f * grow);
            if (lineWidth > 0) {
                Theme.look().shape(canvas, cx - lineWidth / 2, top + lineHeight + 6, lineWidth, 2, 1f, headingColor(colors));
            }
        }

        private void players(Canvas canvas, long t, Look look, int cx, int y) {
            float amount = settle(canvas, t, 650);
            if (amount <= 0) {
                return;
            }
            Palette colors = look.colors();
            int face = 24;
            int gap = 90;
            boolean won = result.result() == MatchEnded.Result.WIN;
            boolean lost = result.result() == MatchEnded.Result.LOSS;

            drawPlayer(canvas, look, PlayerFace.skinOf(selfId), selfName, cx - gap, y, face, won, true);
            canvas.textCentered(Theme.text(Component.literal("VS"), true), cx, y + (face - 8) / 2,
                    colors.textMuted(), look.textShadow());
            if (match != null) {
                drawPlayer(canvas, look, PlayerFace.skinOf(match.opponentId()), match.opponentName(), cx + gap, y,
                        face, lost, false);
            }
            endSettle(canvas);
        }

        private void drawPlayer(Canvas canvas, Look look, net.minecraft.world.entity.player.PlayerSkin skin,
                                String name, int centerX, int y, int face, boolean winner, boolean nameLeft) {
            Palette colors = look.colors();
            Component label = Theme.text(Component.literal(name), true);
            int nameWidth = canvas.textWidth(label);
            int blockWidth = face + 6 + nameWidth;
            int left = centerX - blockWidth / 2;
            int faceX = nameLeft ? left + nameWidth + 6 : left;
            int nameX = nameLeft ? left : left + face + 6;
            if (winner) {
                Theme.look().outline(canvas, faceX - 3, y - 3, face + 6, face + 6, 5f, Math.max(2, guiScaleStroke()),
                        colors.accentStrong());
            }
            canvas.face(skin, faceX, y, face);
            canvas.text(label, nameX, y + (face - 8) / 2, winner ? colors.text() : colors.textMuted(),
                    look.textShadow());
        }

        private int guiScaleStroke() {
            return Math.round((float) minecraft.getWindow().getGuiScale());
        }

        private void score(Canvas canvas, long t, Look look, int cx, int y) {
            float amount = settle(canvas, t, 850);
            if (amount <= 0) {
                return;
            }
            Component text = Theme.text(Component.literal(result.scoreSelf() + "  –  " + result.scoreOpponent()), true, 2f);
            canvas.push();
            canvas.scale(2f, cx, y);
            canvas.textCentered(text, cx, y, look.colors().text(), look.textShadow());
            canvas.pop();
            endSettle(canvas);
        }

        /** The rating counts from its old to its new value; the change chip pulses once it arrives. */
        private void rating(Canvas canvas, long t, Look look, int cx, int y) {
            float amount = settle(canvas, t, 1050);
            if (amount <= 0) {
                return;
            }
            Palette colors = look.colors();
            float count = Easing.OUT_CUBIC.apply(window(t, 1150, 900));
            int rating = Math.round(Lerp.of(result.ratingBefore(), result.ratingAfter(), count));
            int change = result.ratingChange();
            Component value = Theme.text(Component.literal(rating + " MMR"), true);
            Component delta = Theme.text(Component.literal((change >= 0 ? "+" : "") + change), true);
            int changeColor = change > 0 ? colors.positive() : change < 0 ? colors.negative() : colors.textMuted();

            int valueWidth = canvas.textWidth(value);
            int chipWidth = canvas.textWidth(delta) + 10;
            int total = valueWidth + 6 + chipWidth;
            int left = cx - total / 2;
            canvas.text(value, left, y, colors.text(), look.textShadow());

            float pulse = window(t, 2050, 320);
            float chipScale = pulse > 0 && pulse < 1 ? Lerp.of(1f, Motion.PULSE_SCALE, (float) Math.sin(Math.PI * pulse)) : 1f;
            int chipX = left + valueWidth + 6;
            canvas.push();
            canvas.scale(chipScale, chipX + chipWidth / 2f, y + 4);
            Theme.look().shape(canvas, chipX, y - 3, chipWidth, 14, 7f, colors.ghostHovered());
            canvas.text(delta, chipX + 5, y, changeColor, look.textShadow());
            canvas.pop();
            endSettle(canvas);
        }

        private void rank(Canvas canvas, long t, Look look, int cx, int y, long startMs) {
            float amount = settle(canvas, t, startMs);
            if (amount <= 0) {
                return;
            }
            Palette colors = look.colors();
            Component rankText = Theme.text(Component.literal(result.rankAfter()), true);
            if (!result.rankChanged()) {
                canvas.textCentered(rankText, cx, y, colors.textMuted(), look.textShadow());
                endSettle(canvas);
                return;
            }
            boolean up = result.ratingChange() >= 0;
            Component badge = Theme.text(Component.literal(up ? "RANK UP" : "RANK DOWN"), true);
            int badgeWidth = canvas.textWidth(badge) + 12;
            int total = badgeWidth + 6 + canvas.textWidth(rankText);
            int left = cx - total / 2;
            float pulse = window(t, startMs + 200, 360);
            float badgeScale = pulse > 0 && pulse < 1 ? Lerp.of(1f, Motion.PULSE_SCALE, (float) Math.sin(Math.PI * pulse)) : 1f;
            canvas.push();
            canvas.scale(badgeScale, left + badgeWidth / 2f, y + 4);
            Theme.look().shape(canvas, left, y - 3, badgeWidth, 14, 7f, up ? colors.primary() : colors.negative());
            canvas.text(badge, left + 6, y, colors.textOnPrimary(), look.textShadow());
            canvas.pop();
            canvas.text(rankText, left + badgeWidth + 6, y, colors.accentStrong(), look.textShadow());
            endSettle(canvas);
        }

        /** A thin line along the bottom that runs out when the screen continues by itself. */
        private void autoContinue(Canvas canvas, long t, Palette colors) {
            float left = 1 - Math.clamp(t / (float) AUTO_CLOSE_MS, 0f, 1f);
            canvas.fill(x, y + height - 2, Math.round(width * left), 2, colors.accentStrong());
        }

        /** Starts drawing a part that settles in at {@code startMs}; returns how far it has (0 = not yet). */
        private float settle(Canvas canvas, long t, long startMs) {
            float amount = Motion.ENTER.apply(window(t, startMs, 260));
            if (amount <= 0) {
                return 0;
            }
            canvas.push();
            canvas.translate(0, (1 - amount) * 6);
            canvas.pushAlpha(amount);
            return amount;
        }

        private void endSettle(Canvas canvas) {
            canvas.popAlpha();
            canvas.pop();
        }
    }

    private boolean victory() {
        return result.result() == MatchEnded.Result.WIN;
    }

    private static int withAlpha(int argb, int alpha) {
        return alpha << 24 | (argb & 0xFFFFFF);
    }

    /**
     * See-through: the world stays visible behind a gradient that darkens towards the bottom and a soft dark
     * glow behind the content that keeps the text readable. No blur.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            extractPanorama(graphics, partialTick);
        }
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        int bg = Theme.colors().bg0();
        canvas.pushAlpha(openProgress());
        canvas.gradient(0, 0, width, height, withAlpha(bg, 0x38), withAlpha(bg, 0x9E));
        canvas.glow(width / 2, Math.round(height * 0.45f), Math.round(width * 0.42f), Math.round(height * 0.48f),
                withAlpha(bg, 0xA8), 1f);
        canvas.popAlpha();
    }

    /** Linear progress 0..1 of a step that starts at {@code startMs} and lasts {@code durationMs}. */
    private static float window(long t, long startMs, long durationMs) {
        return Math.clamp((t - startMs) / (float) durationMs, 0f, 1f);
    }

    private String headingText() {
        return switch (result.result()) {
            case WIN -> "VICTORY";
            case LOSS -> "DEFEAT";
            case CANCELLED -> "MATCH CANCELLED";
            case OTHER -> "MATCH OVER";
        };
    }

    private int headingColor(Palette colors) {
        return switch (result.result()) {
            case WIN -> colors.accentStrong();
            case LOSS -> colors.negative();
            default -> colors.textMuted();
        };
    }
}
