package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Pill;
import net.am7n.shardclient.shared.ui.Size;
import net.am7n.shardclient.shared.ui.Slider;
import net.am7n.shardclient.shared.ui.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class ModuleCard extends Container {
    private static final int HEADER = 32;
    private static final int PADDING = 8;
    private static final int GAP = 2;

    private final Module module;
    private final Toggle toggle;
    private final List<Element> rows = new ArrayList<>();
    private final List<BooleanSupplier> rowShown = new ArrayList<>();
    private final Tween open = new Tween(0, 220, Motion.MOVE);
    private final Tween hover = new Tween(0, 110, Easing.OUT_CUBIC);
    private final net.am7n.shardclient.shared.ui.Hover hoverFeedback = new net.am7n.shardclient.shared.ui.Hover();
    private final long appearAt;
    private boolean expanded;

    ModuleCard(Module module, int index) {
        this.module = module;
        this.toggle = new Toggle(module::isEnabled, module::setEnabled);
        this.appearAt = Clock.SYSTEM.nanos() + index * Motion.ROW_STAGGER_MS * 1_000_000L;
        add(toggle);
        Element preview = module.preview();
        if (preview != null) {
            addRow(preview, () -> true);
        }
        List<Setting<?>> all = module.settings();
        for (int i = 0; i < all.size(); i++) {
            Setting<?> setting = all.get(i);
            if (setting.heading() != null) {
                List<Setting<?>> group = new ArrayList<>();
                for (int j = i; j < all.size() && (j == i || all.get(j).heading() == null); j++) {
                    group.add(all.get(j));
                }
                addRow(new HeadingRow(setting.heading()), () -> group.stream().anyMatch(Setting::shown));
            }
            addRow(new SettingRow(setting.name(), control(setting)), setting::shown);
        }
        addRow(new SettingRow("Keybind", new KeybindPill(module)), () -> true);
        if (!all.isEmpty()) {
            addRow(new SettingRow("Default settings", new Pill(() -> "Reset", direction -> module.resetSettings())),
                    () -> true);
        }
    }

    private void addRow(Element row, BooleanSupplier shown) {
        rows.add(row);
        rowShown.add(shown);
        add(row);
    }

    private boolean shown(int index) {
        return rowShown.get(index).getAsBoolean();
    }

    private static Element control(Setting<?> setting) {
        return switch (setting) {
            case BooleanSetting bool -> new Toggle(bool::on, bool::set);
            case NumberSetting number -> new Slider(number.min(), number.max(), number::get, number::set, number::display);
            case ChoiceSetting choice -> new Pill(choice::get, choice::cycle);
            default -> throw new IllegalArgumentException("No control for " + setting.getClass());
        };
    }

    private int settingsHeight() {
        int height = PADDING;
        for (int i = 0; i < rows.size(); i++) {
            if (shown(i)) {
                height += rows.get(i).measure().height() + GAP;
            }
        }
        return height;
    }

    @Override
    public Size measure() {
        return new Size(200, HEADER + Math.round(open.get() * settingsHeight()));
    }

    @Override
    protected void layout() {
        Size switchSize = toggle.measure();
        toggle.place(x + width - PADDING - switchSize.width(), y + (HEADER - switchSize.height()) / 2,
                switchSize.width(), switchSize.height());
        int rowY = y + HEADER;
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            boolean visible = shown(i);
            row.visible(visible);
            if (!visible) {
                continue;
            }
            int rowHeight = row.measure().height();
            row.place(x + PADDING + 4, rowY, width - PADDING * 2 - 4, rowHeight);
            rowY += rowHeight + GAP;
        }
    }

    @Override
    public void render(Canvas canvas) {
        float appear = Motion.ENTER.apply(Math.clamp((Clock.SYSTEM.nanos() - appearAt) / (240f * 1_000_000f), 0f, 1f));
        if (appear <= 0) {
            return;
        }
        Look look = Theme.look();
        Palette colors = look.colors();
        boolean shadow = look.textShadow();
        boolean pointed = canvas.mouseY() >= y && canvas.mouseY() < y + HEADER && contains(canvas.mouseX(), canvas.mouseY());
        hoverFeedback.update(pointed);
        hover.set(pointed ? 1 : 0);

        canvas.push();
        canvas.translate(0, (1 - appear) * 6 - net.am7n.shardclient.shared.ui.Hover.lift(hover.get()));
        canvas.pushAlpha(appear);
        Theme.look().shape(canvas, x, y, width, height, 6f, Lerp.color(colors.ghost(), colors.ghostHovered(), hover.get() * 0.6f));
        Theme.look().outline(canvas, x, y, width, height, 6f, 1, module.isEnabled() ? withAlpha(colors.accentStrong(), 0x66)
                : colors.hairline());

        Component name = Theme.text(Component.literal(module.name()), true);
        canvas.text(name, x + PADDING, y + 7, colors.text(), shadow);
        int descriptionWidth = width - PADDING * 3 - toggle.width();
        canvas.text(Theme.text(Component.literal(fit(module.description(), descriptionWidth)), false), x + PADDING,
                y + 18, colors.textMuted(), shadow);
        toggle.render(canvas);

        float openness = open.get();
        if (openness > 0) {
            canvas.clip(x, y + HEADER, width, height - HEADER);
            canvas.pushAlpha(openness);
            for (int i = 0; i < rows.size(); i++) {
                if (shown(i)) {
                    rows.get(i).render(canvas);
                }
            }
            canvas.popAlpha();
            canvas.unclip();
        }
        canvas.popAlpha();
        canvas.pop();
    }

    private static String fit(String text, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(Theme.text(Component.literal(text), false)) <= width) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && font.width(Theme.text(Component.literal(cut + "…"), false)) > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "…";
    }

    private static int withAlpha(int argb, int alpha) {
        return alpha << 24 | (argb & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (toggle.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (mouseY < y + HEADER) {
            if (button == 0) {
                expanded = !expanded;
                open.set(expanded ? 1 : 0);
            } else if (button == 1) {
                module.toggle();
            }
            return true;
        }
        if (expanded) {
            for (int i = 0; i < rows.size(); i++) {
                if (shown(i) && rows.get(i).mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return expanded && super.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key) {
        return expanded && super.keyPressed(key);
    }

    private static final class HeadingRow extends Element {
        private final String label;

        HeadingRow(String label) {
            this.label = label;
        }

        @Override
        public Size measure() {
            return new Size(160, 16);
        }

        @Override
        public void render(Canvas canvas) {
            Look look = Theme.look();
            Component text = Theme.text(Component.literal(label.toUpperCase(java.util.Locale.ROOT)), true);
            int textY = y + height - 10;
            canvas.text(text, x, textY, look.colors().accentStrong(), look.textShadow());
            int lineX = x + canvas.textWidth(text) + 6;
            canvas.fill(lineX, textY + 4, x + width - lineX, 1, look.colors().hairline());
        }
    }
}
