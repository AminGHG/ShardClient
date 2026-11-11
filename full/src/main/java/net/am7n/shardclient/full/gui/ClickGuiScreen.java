package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.hud.HudEditorScreen;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Pill;
import net.am7n.shardclient.shared.ui.ScrollArea;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.am7n.shardclient.shared.ui.Stack;
import net.am7n.shardclient.shared.ui.Toggle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The client menu (Right Shift): modules by category with their switches, settings and keybinds, plus the
 * client's own settings and the HUD editor. It never pauses the game and saves the config when it closes.
 */
public final class ClickGuiScreen extends ShardScreen {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("shardclient-shared", "textures/gui/logo.png");
    private static final int SIDEBAR = 104;
    private static final int TAB_HEIGHT = 20;
    private static final int PADDING = 10;

    private final ModuleManager modules;
    private final ClientConfig config;
    private final String version;

    public ClickGuiScreen(Screen parent, ModuleManager modules, ClientConfig config, String version) {
        super(Component.literal("ShardClient"), parent);
        this.modules = modules;
        this.config = config;
        this.version = version;
    }

    @Override
    protected Element build() {
        return new View();
    }

    @Override
    protected void arrange(Element root, int screenWidth, int screenHeight) {
        int width = Math.clamp(screenWidth - 24, 300, 460);
        int height = Math.clamp(screenHeight - 24, 190, 290);
        root.place((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    @Override
    public void removed() {
        super.removed();
        config.save();
    }

    /** The tabs: one per category, then the client's settings. */
    private record Tab(String label, Category category) {
    }

    private final class View extends Container {

        private final List<Tab> tabs;
        private final Stack list = Stack.column(4);
        private final ScrollArea scroll = new ScrollArea(list, 100);
        private final Button editHud = new Button("Edit HUD", this::openHudEditor);
        private final Tween indicator = new Tween(0, Motion.POPOVER_MS, Motion.MOVE);
        private int selected;

        View() {
            List<Tab> all = new java.util.ArrayList<>();
            for (Category category : Category.values()) {
                all.add(new Tab(category.displayName(), category));
            }
            all.add(new Tab("Settings", null));
            tabs = List.copyOf(all);
            add(scroll);
            add(editHud);
            show(0);
        }

        private void openHudEditor() {
            minecraft.gui.setScreen(new HudEditorScreen(ClickGuiScreen.this, modules, config));
        }

        private void show(int index) {
            selected = index;
            list.children().clear();
            Tab tab = tabs.get(index);
            if (tab.category() == null) {
                buildSettings();
            } else {
                int i = 0;
                for (Module module : modules.in(tab.category())) {
                    list.add(new ModuleCard(module, i++));
                }
            }
            scroll.scrollToTop();
            layout();
        }

        private void buildSettings() {
            list.add(new SettingRow("Look", new Pill(() -> Theme.look().displayName(), direction -> {
                List<net.am7n.shardclient.shared.render.Look> looks = Theme.LOOKS;
                int at = looks.indexOf(Theme.look());
                Theme.use(looks.get(Math.floorMod(at + direction, looks.size())));
            })));
            list.add(new SettingRow("Interface sounds", new Toggle(config::uiSounds, config::setUiSounds)));
            list.add(new SettingRow("Main menu and server list", new Pill(
                    () -> config.menuStyle() == ClientConfig.MenuStyle.SHARD ? "ShardClient" : "Vanilla",
                    direction -> config.setMenuStyle(config.menuStyle() == ClientConfig.MenuStyle.SHARD
                            ? ClientConfig.MenuStyle.VANILLA : ClientConfig.MenuStyle.SHARD))));
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int contentX = x + SIDEBAR + PADDING;
            int contentTop = y + PADDING + 18;
            scroll.place(contentX, contentTop, width - SIDEBAR - PADDING * 2, height - (contentTop - y) - PADDING);
            Size button = editHud.measure();
            editHud.place(x + PADDING, y + height - PADDING - button.height() - 10, SIDEBAR - PADDING * 2,
                    button.height());
        }

        private int tabY(int index) {
            return y + PADDING + 30 + index * (TAB_HEIGHT + 2);
        }

        @Override
        public void render(Canvas canvas) {
            layout();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            look.panel(canvas, x, y, width, height);
            canvas.fill(x + SIDEBAR, y + PADDING, 1, height - PADDING * 2, colors.hairline());

            // Brand
            canvas.image(LOGO, 256, x + PADDING, y + PADDING, 16);
            canvas.text(Theme.text(Component.literal("ShardClient"), true), x + PADDING + 20, y + PADDING + 4,
                    colors.text(), shadow);

            // One highlight that slides between the tabs.
            indicator.set(selected);
            float at = indicator.get();
            int indicatorY = Math.round(tabY(0) + at * (TAB_HEIGHT + 2));
            canvas.smoothRect(x + PADDING - 2, indicatorY, SIDEBAR - PADDING * 2 + 4, TAB_HEIGHT, 5f, colors.ghostHovered());
            canvas.smoothRect(x + PADDING - 2, indicatorY + 5, 2, TAB_HEIGHT - 10, 1f, colors.accentStrong());
            for (int i = 0; i < tabs.size(); i++) {
                boolean active = i == selected;
                canvas.text(Theme.text(Component.literal(tabs.get(i).label()), active), x + PADDING + 6,
                        tabY(i) + 6, active ? colors.text() : colors.textMuted(), shadow);
            }
            canvas.text(Theme.text(Component.literal("v" + version), false), x + PADDING, y + height - PADDING - 7,
                    colors.textDisabled(), shadow);

            // Header of the open tab
            Tab tab = tabs.get(selected);
            String header = tab.label();
            if (tab.category() != null) {
                List<Module> inTab = modules.in(tab.category());
                long on = inTab.stream().filter(Module::isEnabled).count();
                header += "   " + on + " of " + inTab.size() + " on";
            }
            canvas.text(Theme.text(Component.literal(header), true), x + SIDEBAR + PADDING, y + PADDING + 4,
                    colors.text(), shadow);
            canvas.textRight(Theme.text(Component.literal("Right Shift to close"), false), x + width - PADDING,
                    y + PADDING + 4, colors.textDisabled(), shadow);
            super.render(canvas);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x + PADDING && mouseX < x + SIDEBAR - PADDING) {
                for (int i = 0; i < tabs.size(); i++) {
                    if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_HEIGHT) {
                        if (i != selected) {
                            show(i);
                        }
                        return true;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button) || contains(mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int key) {
            if (super.keyPressed(key)) {
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                onClose();
                return true;
            }
            return false;
        }
    }
}
