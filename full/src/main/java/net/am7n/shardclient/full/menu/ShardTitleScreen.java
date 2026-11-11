package net.am7n.shardclient.full.menu;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Icons;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * ShardClient's title screen: Minecraft's own panorama, the brand and the game's four actions centred, the style
 * switch and the client menu as two icons in the bottom-left corner. Multiplayer is the main action.
 */
public final class ShardTitleScreen extends ShardScreen {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("shardclient-shared", "textures/gui/logo.png");
    static final String SHARDPVP_ADDRESS = "shardpvp.xyz";
    private static final int COLUMN = 220;
    private static final int GAP = 6;

    private final long shownAt = Clock.SYSTEM.nanos();

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

    /** The panorama with just enough shade for the text to read. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractPanorama(graphics, partialTick);
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        int bg = Theme.colors().bg0() & 0xFFFFFF;
        canvas.gradient(0, 0, width, height, 0x30 << 24 | bg, 0x90 << 24 | bg);
    }

    private final class View extends Container {

        private final Button multiplayer = new Button("Multiplayer",
                () -> minecraft.gui.setScreen(new JoinMultiplayerScreen(ShardTitleScreen.this)))
                .style(Button.Style.PRIMARY).tall();
        private final Button singleplayer = new Button("Singleplayer",
                () -> minecraft.gui.setScreen(new SelectWorldScreen(ShardTitleScreen.this))).tall();
        private final Button options = new Button("Options",
                () -> minecraft.gui.setScreen(new OptionsScreen(ShardTitleScreen.this, minecraft.options, false)));
        private final Button quit = new Button("Quit", minecraft::stop);
        private final Button style = new Button("Switch to Minecraft's menu", MenuScreens::toggle).iconOnly(Icons.SWAP);
        private final Button client = new Button("Client menu · Right Shift",
                () -> ShardClientFull.get().openMenu(minecraft)).iconOnly(Icons.GRID);
        /** In the order they settle in. */
        private final List<Button> sequence = List.of(multiplayer, singleplayer, options, quit);

        View() {
            sequence.forEach(this::add);
            add(style);
            add(client);
        }

        private int columnTop() {
            return Math.round(height * 0.5f) - 4;
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int left = (width - COLUMN) / 2;
            int y = columnTop();
            multiplayer.place(left, y, COLUMN, 26);
            y += 26 + GAP;
            singleplayer.place(left, y, COLUMN, 26);
            y += 26 + GAP;
            int half = (COLUMN - GAP) / 2;
            options.place(left, y, half, 20);
            quit.place(left + half + GAP, y, COLUMN - half - GAP, 20);
            style.place(8, height - 8 - 20, 20, 20);
            client.place(8 + 20 + 4, height - 8 - 20, 20, 20);
        }

        @Override
        public void render(Canvas canvas) {
            long t = elapsedMs();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            int center = width / 2;
            int brandTop = columnTop() - 84;

            // The brand rises in first.
            float brand = Motion.ENTER.apply(window(t, 0, 420));
            canvas.pushAlpha(brand);
            canvas.push();
            canvas.translate(0, (1 - brand) * 8);
            canvas.image(LOGO, 256, center - 24, brandTop, 48);
            Component name = Theme.text(Component.literal("ShardClient"), true, 2f);
            canvas.push();
            canvas.scale(2f, center, brandTop + 56);
            canvas.textCentered(name, center, brandTop + 56, colors.text(), shadow);
            canvas.pop();
            canvas.pop();
            canvas.popAlpha();

            // Then the actions, one after another.
            for (int i = 0; i < sequence.size(); i++) {
                float appear = Motion.ENTER.apply(window(t, 180 + i * Motion.ROW_STAGGER_MS, 280));
                if (appear <= 0) {
                    continue;
                }
                canvas.pushAlpha(appear);
                canvas.push();
                canvas.translate(0, (1 - appear) * 6);
                sequence.get(i).render(canvas);
                canvas.pop();
                canvas.popAlpha();
            }
            float corner = Motion.ENTER.apply(window(t, 320, 280));
            canvas.pushAlpha(corner);
            style.render(canvas);
            client.render(canvas);
            String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
            canvas.textRight(Theme.text(Component.literal(ShardClientFull.get().runtime().version()
                    + "  ·  " + minecraftVersion), false), width - 8, height - 15, colors.textMuted(), shadow);
            canvas.popAlpha();
        }

        private float window(long t, long start, long duration) {
            return Math.clamp((t - start) / (float) duration, 0f, 1f);
        }
    }
}
