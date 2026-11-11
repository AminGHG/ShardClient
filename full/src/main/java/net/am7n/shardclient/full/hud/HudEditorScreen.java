package net.am7n.shardclient.full.hud;

import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Move HUD elements by dragging them and resize them with the mouse wheel. Elements snap to the screen's centre
 * lines. The element under the pointer follows it exactly while held; positions are saved on close.
 */
public final class HudEditorScreen extends ShardScreen {

    private static final int SNAP = 4;

    private final ModuleManager modules;
    private final ClientConfig config;

    public HudEditorScreen(Screen parent, ModuleManager modules, ClientConfig config) {
        super(Component.literal("Edit HUD"), parent);
        this.modules = modules;
        this.config = config;
    }

    @Override
    protected Element build() {
        return new Editor();
    }

    @Override
    protected void arrange(Element root, int screenWidth, int screenHeight) {
        root.place(0, 0, screenWidth, screenHeight);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        canvas.pushAlpha(openProgress());
        canvas.fill(0, 0, width, height, 0x40000000);
        canvas.popAlpha();
    }

    @Override
    public void removed() {
        super.removed();
        config.save();
    }

    private final class Editor extends Container {

        private final Button done = new Button("Done", HudEditorScreen.this::onClose).style(Button.Style.PRIMARY);
        private HudModule held;
        private double grabX;
        private double grabY;
        private boolean snappedX;
        private boolean snappedY;

        Editor() {
            add(done);
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            Size size = done.measure();
            done.place(x + (width - size.width()) / 2, y + height / 2 + 30, size.width(), size.height());
        }

        @Override
        public void render(Canvas canvas) {
            Look look = Theme.look();
            Palette colors = look.colors();
            if (held != null) {
                if (snappedX) {
                    canvas.fill(width / 2, 0, 1, height, colors.accentStrong());
                }
                if (snappedY) {
                    canvas.fill(0, height / 2, width, 1, colors.accentStrong());
                }
            }
            for (HudModule module : modules.hud()) {
                if (!module.isEnabled()) {
                    continue;
                }
                Size size = module.size(canvas);
                int mx = module.x(width, size.width());
                int my = module.y(height, size.height());
                int w = Math.round(size.width() * module.scaleFactor());
                int h = Math.round(size.height() * module.scaleFactor());
                boolean hovered = canvas.mouseX() >= mx && canvas.mouseX() < mx + w && canvas.mouseY() >= my
                        && canvas.mouseY() < my + h;
                module.render(canvas);
                Theme.look().outline(canvas, mx - 1, my - 1, w + 2, h + 2, 4f, 1,
                        module == held || hovered ? colors.accentStrong() : colors.hairline());
            }
            canvas.textCentered(Theme.text(Component.literal("Drag to move · Scroll to resize"), false), width / 2,
                    height / 2 + 14, colors.text(), true);
            super.render(canvas);
        }

        private HudModule at(Canvas canvas, double px, double py) {
            HudModule found = null;
            for (HudModule module : modules.hud()) {
                if (!module.isEnabled()) {
                    continue;
                }
                Size size = module.size(canvas);
                int mx = module.x(width, size.width());
                int my = module.y(height, size.height());
                if (px >= mx && px < mx + size.width() * module.scaleFactor() && py >= my
                        && py < my + size.height() * module.scaleFactor()) {
                    found = module;
                }
            }
            return found;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            HudModule module = at(sizes(), mouseX, mouseY);
            if (module == null || button != 0) {
                return false;
            }
            Size size = module.size(sizes());
            held = module;
            grabX = mouseX - module.x(width, size.width());
            grabY = mouseY - module.y(height, size.height());
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (held == null) {
                return false;
            }
            Size size = held.size(sizes());
            float w = size.width() * held.scaleFactor();
            float h = size.height() * held.scaleFactor();
            int nx = (int) Math.round(mouseX - grabX);
            int ny = (int) Math.round(mouseY - grabY);
            snappedX = Math.abs(nx + w / 2 - width / 2f) < SNAP;
            snappedY = Math.abs(ny + h / 2 - height / 2f) < SNAP;
            if (snappedX) {
                nx = Math.round(width / 2f - w / 2);
            }
            if (snappedY) {
                ny = Math.round(height / 2f - h / 2);
            }
            held.moveTo(nx, ny, size.width(), size.height(), width, height);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            held = null;
            snappedX = false;
            snappedY = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            HudModule module = at(sizes(), mouseX, mouseY);
            if (module == null) {
                return false;
            }
            var scale = module.settings().stream()
                    .filter(s -> s.id().equals("scale"))
                    .findFirst()
                    .map(s -> (net.am7n.shardclient.full.module.setting.NumberSetting) s)
                    .orElse(null);
            if (scale != null) {
                scale.set(scale.get() + amount * 0.05);
            }
            return true;
        }

        /** Sizes need the font only; a canvas without graphics is enough to measure text. */
        private Canvas sizes() {
            return new Canvas(null, minecraft.font, 0, 0);
        }
    }
}
