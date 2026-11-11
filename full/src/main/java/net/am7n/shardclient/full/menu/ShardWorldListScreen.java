package net.am7n.shardclient.full.menu;

import net.am7n.shardclient.full.mixin.WorldListEntryAccessor;
import net.am7n.shardclient.full.mixin.WorldSelectionListAccessor;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
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
import net.am7n.shardclient.shared.ui.ScrollArea;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.am7n.shardclient.shared.ui.Size;
import net.am7n.shardclient.shared.ui.Stack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList.WorldListEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * ShardClient's world list: the worlds as cards (select with a click, play with a double click) and Minecraft's
 * own two rows of actions at the bottom. Loading, playing, creating, editing, deleting and re-creating all go
 * through Minecraft's own world list, kept alive but never drawn, so every safety prompt (backups, locked or
 * newer worlds, experimental features) stays exactly as in the game.
 */
public final class ShardWorldListScreen extends ShardScreen {

    private static final int CARD_HEIGHT = 46;
    /** Hard drop shadow behind icons: solid black, this many GUI pixels down and right, no blur. */
    private static final int ICON_SHADOW = 2;
    private static final int COLUMN = 440;

    private final Screen parent;
    private WorldSelectionList worlds;
    private List<WorldListEntry> shown = List.of();
    private WorldListEntry selected;
    private View view;

    public ShardWorldListScreen(Screen parent) {
        super(Component.literal("Singleplayer"), parent);
        this.parent = parent;
    }

    @Override
    protected Element build() {
        worlds = new WorldSelectionList.Builder(minecraft, this).width(width).height(height).build();
        view = new View();
        return view;
    }

    @Override
    protected void arrange(Element root, int screenWidth, int screenHeight) {
        int width = Math.min(screenWidth - 24, COLUMN);
        root.place((screenWidth - width) / 2, 10, width, screenHeight - 20);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            extractPanorama(graphics, partialTick);
        }
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        int bg = Theme.colors().bg0() & 0xFFFFFF;
        canvas.pushAlpha(openProgress());
        canvas.gradient(0, 0, width, height, 0xA0 << 24 | bg, 0xD8 << 24 | bg);
        canvas.popAlpha();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    /** Takes over what Minecraft's list loaded since the last check, like it does itself when drawn. */
    private void poll() {
        if (worlds == null) {
            return;
        }
        WorldSelectionListAccessor access = (WorldSelectionListAccessor) worlds;
        List<LevelSummary> levels = access.shardclient$pollLevels();
        if (levels != access.shardclient$displayedLevels()) {
            access.shardclient$handleNewLevels(levels);
        }
        List<WorldListEntry> now = new ArrayList<>();
        for (Object entry : worlds.children()) {
            if (entry instanceof WorldListEntry world) {
                now.add(world);
            }
        }
        if (!sameEntries(now, shown)) {
            shown = now;
            if (selected != null && !shown.contains(selected)) {
                selected = null;
            }
            if (view != null) {
                view.rebuild();
            }
        }
    }

    private static boolean sameEntries(List<WorldListEntry> a, List<WorldListEntry> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean loading() {
        return worlds != null && ((WorldSelectionListAccessor) worlds).shardclient$displayedLevels() == null;
    }

    @Override
    public void tick() {
        super.tick();
        poll();
    }

    // Actions: all Minecraft's own.

    private void play(WorldListEntry entry) {
        if (entry != null && entry.getLevelSummary().primaryActionActive()) {
            entry.joinWorld();
        }
    }

    private void create() {
        CreateWorldScreen.openFresh(minecraft, worlds::returnToScreen);
    }

    private void edit() {
        if (selected != null && selected.getLevelSummary().canEdit()) {
            selected.editWorld();
        }
    }

    private void delete() {
        if (selected != null && selected.getLevelSummary().canDelete()) {
            selected.deleteWorld();
        }
    }

    private void recreate() {
        if (selected != null && selected.getLevelSummary().canRecreate()) {
            selected.recreateWorld();
        }
    }

    private void select(WorldListEntry entry) {
        selected = entry;
        worlds.setSelected(entry);
    }

    private final class View extends Container {

        private final Stack list = Stack.column(4);
        private final ScrollArea scroll = new ScrollArea(list, 100);
        private final Button play = new Button("Play Selected World", () -> play(selected)).style(Button.Style.PRIMARY);
        private final Button create = new Button("Create New World", ShardWorldListScreen.this::create);
        private final Button edit = new Button("Edit", ShardWorldListScreen.this::edit);
        private final Button delete = new Button("Delete", ShardWorldListScreen.this::delete);
        private final Button recreate = new Button("Re-Create", ShardWorldListScreen.this::recreate);
        private final Button back = new Button("Back", ShardWorldListScreen.this::onClose);
        private final List<Button> topRow = List.of(play, create);
        private final List<Button> bottomRow = List.of(edit, delete, recreate, back);

        View() {
            add(scroll);
            topRow.forEach(this::add);
            bottomRow.forEach(this::add);
        }

        void rebuild() {
            list.children().clear();
            int index = 0;
            for (WorldListEntry entry : shown) {
                list.add(new WorldCard(entry, index++));
            }
            layout();
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int listTop = y + 30;
            int actionsTop = y + height - 20 * 2 - 6;
            scroll.place(x, listTop, width, actionsTop - listTop - 8);
            row(topRow, actionsTop);
            row(bottomRow, actionsTop + 26);
        }

        /** Buttons sharing a row's width equally, like Minecraft's own world list. */
        private void row(List<Button> buttons, int top) {
            int gap = 6;
            int each = (width - gap * (buttons.size() - 1)) / buttons.size();
            int bx = x;
            for (int i = 0; i < buttons.size(); i++) {
                int w = i == buttons.size() - 1 ? x + width - bx : each;
                buttons.get(i).place(bx, top, w, 20);
                bx += w + gap;
            }
        }

        @Override
        public void render(Canvas canvas) {
            poll();
            LevelSummary summary = selected == null ? null : selected.getLevelSummary();
            play.enabled(summary != null && summary.primaryActionActive());
            play.label(summary != null ? summary.primaryActionMessage() : LevelSummary.PLAY_WORLD);
            edit.enabled(summary != null && summary.canEdit());
            delete.enabled(summary != null && summary.canDelete());
            recreate.enabled(summary != null && summary.canRecreate());
            layout();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();

            canvas.push();
            canvas.scale(1.5f, x, y + 4);
            canvas.text(Theme.text(Component.literal("Singleplayer"), true, 1.5f), x, y + 4, colors.text(), shadow);
            canvas.pop();
            if (!shown.isEmpty()) {
                canvas.textRight(Component.literal(shown.size() + (shown.size() == 1 ? " world" : " worlds")),
                        x + width, y + 10, colors.textMuted(), true);
            }

            if (shown.isEmpty()) {
                String empty = loading() ? "Loading worlds…" : "No worlds yet.";
                canvas.textCentered(Theme.text(Component.literal(empty), false), x + width / 2, scroll.y() + 30,
                        colors.textMuted(), shadow);
            }
            super.render(canvas);
        }
    }

    private final class WorldCard extends Element {

        private static final int PADDING = 10;

        private final WorldListEntry entry;
        private final long appearAt;
        private final Tween hover = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
        private final Tween selection = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
        private final boolean hasIcon;
        private long lastClick;

        WorldCard(WorldListEntry entry, int index) {
            this.entry = entry;
            this.appearAt = Clock.SYSTEM.nanos() + index * Motion.ROW_STAGGER_MS * 1_000_000L;
            Path icon = entry.getLevelSummary().getIcon();
            this.hasIcon = icon != null && Files.isRegularFile(icon);
        }

        @Override
        public Size measure() {
            return new Size(300, CARD_HEIGHT);
        }

        @Override
        public void render(Canvas canvas) {
            float appear = Motion.ENTER.apply(Math.clamp((Clock.SYSTEM.nanos() - appearAt) / 240_000_000f, 0f, 1f));
            if (appear <= 0) {
                return;
            }
            LevelSummary summary = entry.getLevelSummary();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            hover.set(contains(canvas.mouseX(), canvas.mouseY()) ? 1 : 0);
            selection.set(selected == entry ? 1 : 0);
            float sel = selection.get();

            canvas.pushAlpha(appear);
            canvas.push();
            canvas.translate(0, (1 - appear) * 6);
            int fill = Lerp.color(Lerp.color(colors.ghost(), colors.ghostHovered(), hover.get()),
                    0x2E << 24 | (colors.primary() & 0xFFFFFF), sel);
            Theme.look().shape(canvas, x, y, width, height, 8f, fill);
            Theme.look().outline(canvas, x, y, width, height, 8f, 1, Lerp.color(colors.hairline(), colors.accentStrong(), sel));

            int icon = 32;
            int iconX = x + 7;
            int iconY = y + (height - icon) / 2;
            FaviconTexture texture = ((WorldListEntryAccessor) (Object) entry).shardclient$icon();
            if (hasIcon && texture != null && !texture.isClosed()) {
                canvas.imageWithHardShadow(texture.textureLocation(), 64, iconX, iconY, icon, ICON_SHADOW);
            } else {
                String name = summary.getLevelName();
                Theme.look().shape(canvas, iconX + ICON_SHADOW, iconY + ICON_SHADOW, icon, icon, 4f, 0xFF000000);
                Theme.look().shape(canvas, iconX, iconY, icon, icon, 4f, colors.inset());
                canvas.textCentered(Theme.text(Component.literal(name.isEmpty() ? "?"
                        : name.substring(0, 1).toUpperCase()), true), iconX + icon / 2, iconY + 12,
                        colors.textMuted(), shadow);
            }

            int textX = iconX + icon + 9;
            int right = x + width - PADDING;
            canvas.text(Theme.text(Component.literal(clip(canvas, summary.getLevelName(), right - textX)), true),
                    textX, y + 9, colors.text(), shadow);
            // Details in Minecraft's own font, like the server list's MOTD.
            String date = summary.getLastPlayed() > 0
                    ? WorldSelectionList.DATE_FORMAT.format(Instant.ofEpochMilli(summary.getLastPlayed()).atZone(ZoneId.systemDefault()))
                    : "";
            String idLine = summary.getLevelId() + (date.isEmpty() ? "" : "  ·  " + date);
            canvas.text(Component.literal(clip(canvas, idLine, right - textX)), textX, y + 22, colors.textMuted(), true);
            var info = canvas.wrap(summary.getInfo(), right - textX);
            if (!info.isEmpty()) {
                canvas.line(info.get(0), textX, y + 32, colors.textMuted(), true);
            }

            canvas.pop();
            canvas.popAlpha();
        }

        private String clip(Canvas canvas, String text, int max) {
            if (canvas.textWidth(Component.literal(text)) <= max) {
                return text;
            }
            String out = text;
            while (!out.isEmpty() && canvas.textWidth(Component.literal(out + "…")) > max) {
                out = out.substring(0, out.length() - 1);
            }
            return out + "…";
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !contains(mouseX, mouseY)) {
                return false;
            }
            long now = System.currentTimeMillis();
            if (selected == entry && now - lastClick < 300) {
                play(entry);
            }
            select(entry);
            lastClick = now;
            return true;
        }
    }
}
