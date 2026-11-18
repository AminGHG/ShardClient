package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.hud.HudEditorScreen;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.anim.Lerp;
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
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ClickGuiScreen extends ShardScreen {
    private static final Asset LOGO = Asset.shared("textures/gui/logo.png");
    private static final int SIDEBAR = 104;
    private static final int TAB_HEIGHT = 20;
    private static final int PADDING = 10;

    private final ModuleManager modules;
    private final ClientConfig config;
    public ClickGuiScreen(Screen parent, ModuleManager modules, ClientConfig config) {
        super(Component.literal("ShardClient"), parent);
        this.modules = modules;
        this.config = config;
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

    private record Tab(String label, Category category) {
    }

    private final class View extends Container {
        private final List<Tab> tabs;
        private final Stack list = Stack.column(4);
        private final ScrollArea scroll = new ScrollArea(list, 100);
        private final Button editHud = new Button("Edit HUD", this::openHudEditor);
        private final Tween indicator = new Tween(0, Motion.POPOVER_MS, Motion.MOVE);
        private int selected;
        private final Tween[] tabHover;
        private final net.am7n.shardclient.shared.ui.Hover[] tabFeedback;

        View() {
            List<Tab> all = new java.util.ArrayList<>();
            for (Category category : Category.values()) {
                all.add(new Tab(category.displayName(), category));
            }
            all.add(new Tab("Settings", null));
            tabs = List.copyOf(all);
            tabHover = new Tween[tabs.size()];
            tabFeedback = new net.am7n.shardclient.shared.ui.Hover[tabs.size()];
            for (int i = 0; i < tabs.size(); i++) {
                tabHover[i] = new Tween(0, Theme.FAST_MS, net.am7n.shardclient.shared.anim.Easing.OUT_CUBIC);
                tabFeedback[i] = new net.am7n.shardclient.shared.ui.Hover();
            }
            add(scroll);
            add(editHud);
            add(search);
            show(0);
        }

        private final net.am7n.shardclient.shared.ui.SearchField search =
                new net.am7n.shardclient.shared.ui.SearchField("Search modules", this::query);

        private List<Module> results;

        private void query(String text) {
            String needle = text.trim().toLowerCase(java.util.Locale.ROOT);
            if (needle.isEmpty()) {
                results = null;
                show(selected);
                return;
            }
            results = modules.all().stream()
                    .filter(m -> m.name().toLowerCase(java.util.Locale.ROOT).contains(needle)
                            || m.description().toLowerCase(java.util.Locale.ROOT).contains(needle)
                            || m.category().displayName().toLowerCase(java.util.Locale.ROOT).contains(needle))

                    .sorted(java.util.Comparator
                            .comparing((Module m) -> !m.name().toLowerCase(java.util.Locale.ROOT).contains(needle))
                            .thenComparing(Module::name))
                    .toList();
            list.children().clear();
            int i = 0;
            for (Module module : results) {
                list.add(new ModuleCard(module, i++));
            }
            scroll.scrollToTop();
            layout();
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
            list.add(new SettingRow("Style", new Pill(() -> Theme.style().displayName(), direction -> {
                Theme.Style[] styles = Theme.Style.values();
                Theme.useStyle(styles[Math.floorMod(Theme.style().ordinal() + direction, styles.length)]);
            })));

            list.add(new SettingRow("Light mode", new Toggle(Theme::lightMode, Theme::useLightMode)
                    .enabledWhen(Theme::lightModeAvailable)));
            list.add(new SettingRow("Interface sounds", new Toggle(config::uiSounds, config::setUiSounds)));
            list.add(new SettingRow("Hover effects", new Toggle(config::hoverEffects, config::setHoverEffects)));
            list.add(new SettingRow("Hover sounds", new Toggle(config::hoverSounds, config::setHoverSounds)));
            list.add(new SettingRow("Badges on every server", new Toggle(config::badgesEverywhere,
                    config::setBadgesEverywhere)));
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
            int searchWidth = Math.min(130, (width - SIDEBAR - PADDING * 2) / 2);
            search.place(x + width - PADDING - searchWidth, y + PADDING - 1, searchWidth, 18);
            Size button = editHud.measure();
            editHud.place(x + PADDING, y + height - PADDING - button.height(), SIDEBAR - PADDING * 2,
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

            canvas.image(LOGO, 256, x + PADDING, y + PADDING, 16);
            canvas.text(Theme.text(Component.literal("ShardClient"), true), x + PADDING + 20, y + PADDING + 4,
                    colors.text(), shadow);

            indicator.set(selected);
            float at = indicator.get();
            int indicatorY = Math.round(tabY(0) + at * (TAB_HEIGHT + 2));
            Theme.look().shape(canvas, x + PADDING - 2, indicatorY, SIDEBAR - PADDING * 2 + 4, TAB_HEIGHT, 5f, colors.ghostHovered());
            Theme.look().shape(canvas, x + PADDING - 2, indicatorY + 5, 2, TAB_HEIGHT - 10, 1f, colors.accentStrong());
            for (int i = 0; i < tabs.size(); i++) {
                boolean active = i == selected;
                boolean pointed = canvas.mouseX() >= x + PADDING - 2 && canvas.mouseX() < x + SIDEBAR - PADDING + 2
                        && canvas.mouseY() >= tabY(i) && canvas.mouseY() < tabY(i) + TAB_HEIGHT;
                tabFeedback[i].update(pointed && !active);
                tabHover[i].set(pointed && !active ? 1 : 0);
                float hover = tabHover[i].get();
                if (hover > 0) {
                    canvas.pushAlpha(hover * 0.6f);
                    Theme.look().shape(canvas, x + PADDING - 2, tabY(i), SIDEBAR - PADDING * 2 + 4, TAB_HEIGHT, 5f,
                            colors.ghostHovered());
                    canvas.popAlpha();
                }
                int nudge = net.am7n.shardclient.shared.ui.Hover.effects() ? Math.round(hover * 2) : 0;
                int color = active ? colors.text() : Lerp.color(colors.textMuted(), colors.text(), hover);
                canvas.text(Theme.text(Component.literal(tabs.get(i).label()), active), x + PADDING + 6 + nudge,
                        tabY(i) + 6, color, shadow);
            }

            Tab tab = tabs.get(selected);
            String header = tab.label();
            if (results != null) {
                header = results.isEmpty() ? "No modules found" : results.size() == 1 ? "1 module" : results.size() + " modules";
            } else if (tab.category() != null) {
                List<Module> inTab = modules.in(tab.category());
                long on = inTab.stream().filter(Module::isEnabled).count();
                header += "   " + on + " of " + inTab.size() + " on";
            }
            canvas.text(Theme.text(Component.literal(header), true), x + SIDEBAR + PADDING, y + PADDING + 4,
                    colors.text(), shadow);
            super.render(canvas);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!search.contains(mouseX, mouseY)) {
                search.focus(false);
            }
            if (mouseX >= x + PADDING && mouseX < x + SIDEBAR - PADDING) {
                for (int i = 0; i < tabs.size(); i++) {
                    if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_HEIGHT) {
                        if (results != null) {
                            selected = i;
                            search.clear();
                        } else if (i != selected) {
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

            if (key == GLFW.GLFW_KEY_F
                    && (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS)) {
                search.focus(true);
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
