package net.am7n.shardclient.full.platform;

import net.am7n.shardclient.full.mixin.WorldListEntryAccessor;
import net.am7n.shardclient.full.mixin.WorldSelectionListAccessor;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class WorldList {
    public static final Component PLAY_WORLD = Component.translatable("selectWorld.select");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();

    public static final class Entry {
        private final WorldSelectionList.WorldListEntry raw;

        private Entry(WorldSelectionList.WorldListEntry raw) {
            this.raw = raw;
        }

        public LevelSummary summary() {
            return ((WorldListEntryAccessor) (Object) raw).shardclient$summary();
        }

        public boolean canPlay() {
            return !summary().isDisabled();
        }

        public Component playLabel() {
            return PLAY_WORLD;
        }

        public boolean canEdit() {
            return !summary().isDisabled();
        }

        public boolean canDelete() {
            return !summary().isLocked();
        }

        public boolean canRecreate() {
            return !summary().isDisabled();
        }

        public void join() {
            raw.joinWorld();
        }

        public void edit() {
            raw.editWorld();
        }

        public void delete() {
            raw.deleteWorld();
        }

        public void recreate() {
            raw.recreateWorld();
        }

        public Asset icon() {
            FaviconTexture texture = ((WorldListEntryAccessor) (Object) raw).shardclient$icon();
            return texture == null ? null : Assets.asset(texture.textureLocation());
        }
    }

    public interface Host {
        WorldSelectionList shardclient$list();
    }

    private final SelectWorldScreen host;
    private final WorldSelectionList list;
    private final Map<WorldSelectionList.WorldListEntry, Entry> entries = new IdentityHashMap<>();

    public WorldList(Screen screen, Screen parent, int width, int height) {
        host = new SelectWorldScreen(parent);
        host.init(Minecraft.getInstance(), width, height);
        list = ((Host) host).shardclient$list();
    }

    public boolean loading() {
        return ((WorldSelectionListAccessor) list).shardclient$displayedLevels() == null;
    }

    public List<Entry> poll() {
        WorldSelectionListAccessor access = (WorldSelectionListAccessor) list;
        List<LevelSummary> levels = access.shardclient$pollLevels();
        if (levels != access.shardclient$displayedLevels()) {
            access.shardclient$handleNewLevels(levels);
        }
        List<Entry> now = new ArrayList<>();
        for (Object child : list.children()) {
            if (child instanceof WorldSelectionList.WorldListEntry world) {
                now.add(entries.computeIfAbsent(world, Entry::new));
            }
        }
        entries.keySet().removeIf(raw -> now.stream().noneMatch(e -> e.raw == raw));
        return now;
    }

    public void select(Entry entry) {
        list.setSelected(entry == null ? null : entry.raw);
    }

    public void createNew() {
        CreateWorldScreen.openFresh(Minecraft.getInstance(), host);
    }

    public static String date(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }
}
