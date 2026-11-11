package net.am7n.shardclient.full.menu;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.practice.ui.ResultEffects;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Pill;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * ShardClient's title screen, after the ShardPvP website's hero: brand and actions on the left over the
 * panorama, a violet glow and drifting squares, the style switch in the bottom-left corner.
 */
public final class ShardTitleScreen extends ShardScreen {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("shardclient-shared", "textures/gui/logo.png");
    static final String SHARDPVP_ADDRESS = "shardpvp.xyz";
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_GAP = 4;

    private final long shownAt = Clock.SYSTEM.nanos();
    private final ResultEffects ambience = new ResultEffects(true);

    public ShardTitleScreen() {
        super(Component.literal("ShardClient"), null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractPanorama(graphics, partialTick);
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        int bg = Theme.colors().bg0() & 0xFFFFFF;
        canvas.gradient(0, 0, width, height, 0x66 << 24 | bg, 0xD0 << 24 | bg);
        canvas.glow(Math.round(width * 0.22f), Math.round(height * 0.42f), Math.round(width * 0.42f),
                Math.round(height * 0.6f), 0x40 << 24 | (Theme.colors().primary() & 0xFFFFFF), 1f);
        ambience.drift(canvas, Theme.colors(), elapsedMs(), 1f);
    }

    private void playShardPvp() {
        ServerData server = new ServerData("ShardPvP", SHARDPVP_ADDRESS, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(SHARDPVP_ADDRESS), server, false, null);
    }

    private final class View extends Container {

        private final List<Button> buttons = List.of(
                new Button("Play ShardPvP", ShardTitleScreen.this::playShardPvp).style(Button.Style.PRIMARY),
                new Button("Singleplayer", () -> minecraft.gui.setScreen(new SelectWorldScreen(ShardTitleScreen.this))),
                new Button("Multiplayer", () -> minecraft.gui.setScreen(new JoinMultiplayerScreen(ShardTitleScreen.this))),
                new Button("Options", () -> minecraft.gui.setScreen(new OptionsScreen(ShardTitleScreen.this, minecraft.options, false))),
                new Button("Quit", minecraft::stop));
        private final Pill style = new Pill(() -> "Style: ShardClient", direction -> MenuScreens.toggle());
        private final Pill client = new Pill(() -> "Client menu", direction -> ShardClientFull.get().openMenu(minecraft));

        View() {
            buttons.forEach(this::add);
            add(style);
            add(client);
        }

        private int left() {
            return Math.max(20, Math.round(width * 0.08f));
        }

        private int buttonsTop() {
            return Math.max(70, Math.round(height * 0.42f));
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int y = buttonsTop();
            for (Button button : buttons) {
                button.place(left(), y, BUTTON_WIDTH, 20);
                y += 20 + BUTTON_GAP;
            }
            Size styleSize = style.measure();
            style.place(8, height - 8 - styleSize.height(), styleSize.width(), styleSize.height());
            Size clientSize = client.measure();
            client.place(width - 8 - clientSize.width(), 8, clientSize.width(), clientSize.height());
        }

        @Override
        public void render(Canvas canvas) {
            long t = elapsedMs();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            int left = left();
            int brandTop = buttonsTop() - 64;

            // Brand: logo and name rise in, then the tagline.
            float brand = Motion.ENTER.apply(window(t, 0, 500));
            canvas.pushAlpha(brand);
            canvas.push();
            canvas.translate(0, (1 - brand) * 8);
            canvas.image(LOGO, 256, left, brandTop, 30);
            Component name = Theme.text(Component.literal("ShardClient"), true);
            canvas.push();
            canvas.scale(2.2f, left + 36, brandTop + 4);
            canvas.text(name, left + 36, brandTop + 4, colors.text(), shadow);
            canvas.pop();
            canvas.pop();
            canvas.popAlpha();
            float tagline = Motion.ENTER.apply(window(t, 150, 400));
            canvas.pushAlpha(tagline);
            canvas.text(Theme.text(Component.literal("Minecraft PvP, made for ShardPvP"), false), left, brandTop + 40,
                    colors.textMuted(), shadow);
            canvas.popAlpha();

            // Buttons one after another.
            for (int i = 0; i < buttons.size(); i++) {
                float appear = Motion.ENTER.apply(window(t, 250 + i * Motion.ROW_STAGGER_MS * 2, 300));
                if (appear <= 0) {
                    continue;
                }
                canvas.pushAlpha(appear);
                canvas.push();
                canvas.translate(Lerp.of(-8, 0, appear), 0);
                buttons.get(i).render(canvas);
                canvas.pop();
                canvas.popAlpha();
            }
            style.render(canvas);
            client.render(canvas);

            String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
            canvas.textRight(Theme.text(Component.literal("ShardClient " + ShardClientFull.get().runtime().version()
                    + "  ·  Minecraft " + minecraftVersion), false), width - 8, height - 15, colors.textDisabled(), shadow);
        }

        private float window(long t, long start, long duration) {
            return Math.clamp((t - start) / (float) duration, 0f, 1f);
        }
    }
}
