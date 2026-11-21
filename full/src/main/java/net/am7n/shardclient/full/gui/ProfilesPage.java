package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.config.Profiles;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Icons;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Button;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Hover;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ProfilesPage extends Container {
    private static final int FIELD = 20;
    private static final int MESSAGE = 16;
    private static final int ROW = 26;
    private static final int GAP = 4;
    private static final long CONFIRM_MS = 3000;

    private final ClientConfig config;
    private final Profiles profiles;
    private final NameField name;
    private final Button create;
    private final Button blank;
    private final List<Row> rows = new ArrayList<>();
    private String message;
    private boolean error;
    private String armed;
    private long armedUntil;
    private String renaming;

    ProfilesPage(ClientConfig config) {
        this.config = config;
        this.profiles = config.profiles();
        this.name = new NameField("New profile name", Profiles.MAX_NAME, text -> make(false), () -> { });
        this.create = new Button("Create", () -> make(false)).style(Button.Style.PRIMARY).icon(Icons.PLUS);
        this.blank = new Button("Blank", () -> make(true));
        profiles.rescan();
        rebuild();
    }

    private void rebuild() {
        children.clear();
        rows.clear();
        add(name);
        add(create);
        add(blank);
        int i = 0;
        for (String profile : profiles.names()) {
            Row row = new Row(profile, i++);
            rows.add(row);
            add(row);
        }
        if (width > 0) {
            layout();
        }
    }

    private void say(String text, boolean problem) {
        message = text;
        error = problem;
    }

    private void make(boolean fromDefaults) {
        String wanted = name.text();
        String problem = profiles.create(wanted, fromDefaults);
        if (problem != null) {
            say(problem, true);
            name.focus(true);
            return;
        }
        config.save();
        say(fromDefaults ? "Made " + wanted.strip() + " with default settings and switched to it"
                : "Saved your settings as " + wanted.strip() + " and switched to it", false);
        name.text("").focus(false);
        rebuild();
    }

    private void use(String profile) {
        if (profiles.isActive(profile)) {
            return;
        }
        profiles.use(profile);
        config.save();
        say("Now using " + profile, false);
        rebuild();
    }

    private void copy(String profile) {
        String problem = profiles.duplicate(profile);
        say(problem != null ? problem : "Copied " + profile, problem != null);
        rebuild();
    }

    private void remove(String profile) {
        long now = System.currentTimeMillis();
        if (!profile.equals(armed) || now > armedUntil) {
            armed = profile;
            armedUntil = now + CONFIRM_MS;
            say("Click the bin again to delete " + profile, true);
            return;
        }
        armed = null;
        boolean wasActive = profiles.isActive(profile);
        String problem = profiles.delete(profile);
        if (problem == null) {
            config.save();
            say(wasActive ? "Deleted " + profile + ", now using " + profiles.active() : "Deleted " + profile, false);
        } else {
            say(problem, true);
        }
        rebuild();
    }

    private void startRename(String profile) {
        renaming = profile;
        rebuild();
    }

    private void finishRename(String profile, String to) {
        String problem = profiles.rename(profile, to);
        if (problem != null) {
            say(problem, true);
            return;
        }
        renaming = null;
        config.save();
        say("Renamed to " + to.strip(), false);
        rebuild();
    }

    private void cancelRename() {
        renaming = null;
        rebuild();
    }

    @Override
    public Size measure() {
        return new Size(200, FIELD + MESSAGE + rows.size() * (ROW + GAP));
    }

    @Override
    protected void layout() {
        Size blankSize = blank.measure();
        Size createSize = create.measure();
        int right = x + width;
        blank.place(right - blankSize.width(), y, blankSize.width(), FIELD);
        right -= blankSize.width() + GAP;
        create.place(right - createSize.width(), y, createSize.width(), FIELD);
        right -= createSize.width() + GAP;
        name.place(x, y, Math.max(40, right - x), FIELD);
        int rowY = y + FIELD + MESSAGE;
        for (Row row : rows) {
            row.place(x, rowY, width, ROW);
            rowY += ROW + GAP;
        }
    }

    @Override
    public void render(Canvas canvas) {
        layout();
        Look look = Theme.look();
        Palette colors = look.colors();
        if (armed != null && System.currentTimeMillis() > armedUntil) {
            armed = null;
            message = null;
        }
        String line = message != null ? message
                : "Create copies your settings, Blank uses defaults";
        int color = message == null ? colors.textDisabled() : error ? colors.textMuted() : colors.accentStrong();
        canvas.text(Theme.text(Component.literal(fit(line, width)), false), x + 1, y + FIELD + 5, color,
                look.textShadow());
        super.render(canvas);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!name.contains(mouseX, mouseY)) {
            name.focus(false);
        }
        if (renaming != null) {
            for (Row row : rows) {
                if (row.field != null && !row.field.contains(mouseX, mouseY)) {
                    renaming = null;
                    rebuild();
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    private final class Row extends Container {
        private static final int PADDING = 8;
        private static final int ICON = 20;

        private final String profile;
        private final boolean active;
        private final Button use;
        private final Button rename;
        private final Button duplicate;
        private final Button delete;
        private final NameField field;
        private final Tween hover = new Tween(0, 110, Easing.OUT_CUBIC);
        private final Hover hoverFeedback = new Hover();
        private final long appearAt;

        Row(String profile, int index) {
            this.profile = profile;
            this.active = profiles.isActive(profile);
            this.appearAt = Clock.SYSTEM.nanos() + index * Motion.ROW_STAGGER_MS * 1_000_000L;
            this.use = new Button("Use this profile", () -> use(profile)).iconOnly(Icons.PLAY);
            this.rename = new Button("Rename", () -> startRename(profile)).iconOnly(Icons.PENCIL);
            this.duplicate = new Button("Copy", () -> copy(profile)).iconOnly(Icons.COPY);
            this.delete = new Button("Delete", () -> remove(profile)).iconOnly(Icons.TRASH);
            boolean editing = profile.equals(renaming);
            this.field = editing ? new NameField(profile, Profiles.MAX_NAME, to -> finishRename(profile, to),
                    ProfilesPage.this::cancelRename).text(profile).focus(true) : null;
            if (!active) {
                add(use);
            }
            add(rename);
            add(duplicate);
            add(delete);
            if (field != null) {
                add(field);
            }
        }

        @Override
        public Size measure() {
            return new Size(200, ROW);
        }

        @Override
        protected void layout() {
            int buttonY = y + (height - ICON) / 2;
            int right = x + width - 3;
            for (Button button : new Button[]{delete, duplicate, rename, use}) {
                if (button == use && active) {
                    continue;
                }
                right -= ICON;
                button.place(right, buttonY, ICON, ICON);
                right -= 2;
            }
            if (field != null) {
                field.place(x + 3, buttonY, Math.max(40, right - x - 6), ICON);
            }
        }

        private int buttonsLeft() {
            return x + width - 3 - (active ? 3 : 4) * (ICON + 2);
        }

        @Override
        public void render(Canvas canvas) {
            layout();
            float appear = Motion.ENTER.apply(Maths.clamp((Clock.SYSTEM.nanos() - appearAt) / (240f * 1_000_000f), 0f, 1f));
            if (appear <= 0) {
                return;
            }
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean pointed = field == null && contains(canvas.mouseX(), canvas.mouseY())
                    && canvas.mouseX() < buttonsLeft() && !active;
            hoverFeedback.update(pointed);
            hover.set(pointed ? 1 : 0);

            canvas.push();
            canvas.translate(0, (1 - appear) * 6 - Hover.lift(hover.get()));
            canvas.pushAlpha(appear);
            look.shape(canvas, x, y, width, height, 6f, Lerp.color(colors.ghost(), colors.ghostHovered(), hover.get() * 0.6f));
            look.outline(canvas, x, y, width, height, 6f, 1, active ? 0x66 << 24 | (colors.accentStrong() & 0xFFFFFF)
                    : colors.hairline());
            if (field == null) {
                int textX = x + PADDING;
                int textY = y + (height - 8) / 2;
                int room = buttonsLeft() - textX - 6;
                String badge = active ? "Active" : profile.equals(armed) ? "Click bin again" : null;
                int badgeWidth = badge == null ? 0 : canvas.textWidth(Theme.text(Component.literal(badge), true)) + 10;
                Component shown = Theme.text(Component.literal(fit(profile, room - badgeWidth - (badge == null ? 0 : 6))), true);
                canvas.text(shown, textX, textY, colors.text(), look.textShadow());
                if (badge != null) {
                    int badgeX = textX + canvas.textWidth(shown) + 6;
                    int tone = active ? colors.accentStrong() : colors.textMuted();
                    look.shape(canvas, badgeX, y + (height - 12) / 2, badgeWidth, 12, 4f, 0x33 << 24 | (tone & 0xFFFFFF));
                    canvas.text(Theme.text(Component.literal(badge), true), badgeX + 5, y + (height - 8) / 2 + 1, tone,
                            false);
                }
            }
            super.render(canvas);
            canvas.popAlpha();
            canvas.pop();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (field == null && button == 0 && mouseX < buttonsLeft()) {
                use(profile);
            }
            return true;
        }
    }
}
