package net.am7n.shardclient.full.menu;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.anim.Clock;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Icons;
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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ShardClient's server list: tabs for favourites, all servers and recently joined ones; the servers as cards
 * (select with a click, join with a double click, star to favourite); and Minecraft's own two rows of actions
 * at the bottom. Servers, pings and the add / edit / delete / direct-connect dialogs are Minecraft's own.
 */
public final class ShardServerListScreen extends ShardScreen {

    private static final int CARD_HEIGHT = 46;
    private static final int PADDING = 10;
    private static final int COLUMN = 440;
    /** Width of the MOTD in Minecraft's list: its 305 px row less 4 px padding, the 32 px icon and a 2 px gap. */
    private static final int VANILLA_MOTD_WIDTH = 267;
    private static final Identifier[] PING = sprites("server_list/ping_", 5);
    private static final Identifier[] PINGING = sprites("server_list/pinging_", 5);
    private static final Identifier INCOMPATIBLE = Identifier.withDefaultNamespace("server_list/incompatible");
    private static final Identifier UNREACHABLE = Identifier.withDefaultNamespace("server_list/unreachable");

    private static Identifier[] sprites(String prefix, int count) {
        Identifier[] out = new Identifier[count];
        for (int i = 0; i < count; i++) {
            out[i] = Identifier.withDefaultNamespace(prefix + (i + 1));
        }
        return out;
    }

    private enum Tab {
        ALL("All servers"),
        RECENT("Recent");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private final ServerList servers;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final ServerData featured = new ServerData("ShardPvP", ShardTitleScreen.SHARDPVP_ADDRESS, ServerData.Type.OTHER);
    private final Map<ServerData, Icon> icons = new HashMap<>();
    /** Servers joined by direct connection, kept for the Recent tab (not in the saved list). */
    private final Map<String, ServerData> recentData = new HashMap<>();
    private final ClientConfig config = ShardClientFull.get().config();
    private View view;
    private Tab tab = Tab.ALL;
    private ServerData selected;

    public ShardServerListScreen(Screen parent) {
        super(Component.literal("Multiplayer"), parent);
        this.parent = parent;
        this.servers = new ServerList(net.minecraft.client.Minecraft.getInstance());
        this.servers.load();
    }

    @Override
    protected Element build() {
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
    public void tick() {
        super.tick();
        pinger.tick();
    }

    @Override
    public void removed() {
        super.removed();
        pinger.removeAll();
        icons.values().forEach(Icon::close);
        icons.clear();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private void returnHere() {
        servers.load();
        selected = null;
        minecraft.gui.setScreen(this);
        if (view != null) {
            view.rebuild();
        }
    }

    private boolean isFeatured(ServerData server) {
        return isFeatured(server.ip);
    }

    private boolean isFeatured(String address) {
        return address.equalsIgnoreCase(featured.ip);
    }

    // Actions

    private void join(ServerData server) {
        if (server == null) {
            return;
        }
        config.markJoined(server.name, server.ip);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(server.ip), server, false, null);
    }

    private void add() {
        ServerData server = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
        minecraft.gui.setScreen(new ManageServerScreen(this, Component.literal("Add Server"), accepted -> {
            if (accepted) {
                servers.add(server, false);
                servers.save();
            }
            returnHere();
        }, server));
    }

    private void edit() {
        ServerData server = selected;
        if (server == null || server == featured) {
            return;
        }
        minecraft.gui.setScreen(new ManageServerScreen(this, Component.literal("Edit Server Info"), accepted -> {
            if (accepted) {
                servers.save();
            } else {
                servers.load();
            }
            returnHere();
        }, server));
    }

    private void delete() {
        ServerData server = selected;
        if (server == null || server == featured) {
            return;
        }
        minecraft.gui.setScreen(new ConfirmScreen(accepted -> {
            if (accepted) {
                servers.remove(server);
                servers.save();
            }
            returnHere();
        }, Component.literal("Remove " + server.name + "?"),
                Component.literal("It will be removed from your server list."),
                Component.literal("Remove"), Component.literal("Cancel")));
    }

    private void directConnect() {
        ServerData server = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
        minecraft.gui.setScreen(new DirectJoinServerScreen(this, accepted -> {
            if (accepted) {
                join(server);
            } else {
                minecraft.gui.setScreen(this);
            }
        }, server));
    }

    private void refresh() {
        pinger.removeAll();
        icons.values().forEach(Icon::close);
        icons.clear();
        featured.setState(ServerData.State.INITIAL);
        for (int i = 0; i < servers.size(); i++) {
            servers.get(i).setState(ServerData.State.INITIAL);
        }
        view.rebuild();
    }

    /** Starts a ping the first time a server is shown, like Minecraft's list does. */
    private void ping(ServerData server) {
        if (server.state() != ServerData.State.INITIAL) {
            return;
        }
        server.setState(ServerData.State.PINGING);
        server.motd = Component.empty();
        server.status = Component.empty();
        Util.backgroundExecutor().execute(() -> {
            try {
                pinger.pingServer(server, () -> minecraft.execute(servers::save), () -> { },
                        EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
            } catch (UnknownHostException ex) {
                server.setState(ServerData.State.UNREACHABLE);
                server.motd = Component.literal("Can't resolve hostname");
            } catch (Exception ex) {
                server.setState(ServerData.State.UNREACHABLE);
                server.motd = Component.literal("Can't connect to server");
            }
        });
    }

    /** The servers the open tab shows, ShardPvP first where it belongs. */
    private List<ServerData> entries() {
        List<ServerData> saved = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            saved.add(servers.get(i));
        }
        List<ServerData> out = new ArrayList<>();
        boolean featuredSaved = saved.stream().anyMatch(this::isFeatured);
        switch (tab) {
            case ALL -> {
                if (!featuredSaved) {
                    out.add(featured);
                }
                out.addAll(saved);
            }
            case RECENT -> {
                // Every server joined, newest first: saved ones as they are, direct connections remembered.
                for (ClientConfig.RecentServer recent : config.recentServers()) {
                    ServerData known = saved.stream().filter(s -> s.ip.equalsIgnoreCase(recent.address()))
                            .findFirst().orElse(isFeatured(recent.address()) ? featured : null);
                    out.add(known != null ? known : recentData.computeIfAbsent(recent.address().toLowerCase(),
                            address -> new ServerData(recent.name(), recent.address(), ServerData.Type.OTHER)));
                }
            }
        }
        return out;
    }

    /** A server's icon as a texture, re-uploaded when the server sends a different one. */
    private final class Icon implements AutoCloseable {
        private final FaviconTexture texture;
        private byte[] uploaded;

        Icon(ServerData server) {
            texture = FaviconTexture.forServer(minecraft.getTextureManager(), server.ip);
        }

        boolean update(byte[] bytes) {
            if (bytes == null) {
                return false;
            }
            if (!Arrays.equals(bytes, uploaded)) {
                try {
                    texture.upload(NativeImage.read(bytes));
                    uploaded = bytes;
                } catch (Exception ex) {
                    ShardClientRuntime.LOGGER.debug("Invalid server icon", ex);
                    return false;
                }
            }
            return true;
        }

        @Override
        public void close() {
            texture.close();
        }
    }

    private final class View extends Container {

        private static final int TAB_HEIGHT = 22;
        private static final int TAB_WIDTH = 92;

        private final Stack list = Stack.column(4);
        private final ScrollArea scroll = new ScrollArea(list, 100);
        private final Tween tabIndicator = new Tween(Tab.ALL.ordinal(), Motion.POPOVER_MS, Motion.MOVE);
        private final Button join = new Button("Join Server", () -> join(selected)).style(Button.Style.PRIMARY).icon(Icons.PLAY);
        private final Button direct = new Button("Direct Connection", ShardServerListScreen.this::directConnect).icon(Icons.LINK);
        private final Button add = new Button("Add Server", ShardServerListScreen.this::add).icon(Icons.PLUS);
        private final Button edit = new Button("Edit", ShardServerListScreen.this::edit).icon(Icons.PENCIL);
        private final Button delete = new Button("Delete", ShardServerListScreen.this::delete).icon(Icons.TRASH);
        private final Button refresh = new Button("Refresh", ShardServerListScreen.this::refresh).icon(Icons.REFRESH);
        private final Button back = new Button("Back", ShardServerListScreen.this::onClose).icon(Icons.BACK);
        private final List<Button> topRow = List.of(join, direct, add);
        private final List<Button> bottomRow = List.of(edit, delete, refresh, back);

        View() {
            add(scroll);
            topRow.forEach(this::add);
            bottomRow.forEach(this::add);
            rebuild();
        }

        void rebuild() {
            list.children().clear();
            int index = 0;
            for (ServerData server : entries()) {
                list.add(new ServerCard(server, index++));
            }
            if (selected != null && entries().stream().noneMatch(s -> s == selected)) {
                selected = null;
            }
            scroll.scrollToTop();
            layout();
        }

        private void showTab(Tab next) {
            if (next != tab) {
                tab = next;
                rebuild();
            }
        }

        private int tabsTop() {
            return y + 26;
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int listTop = tabsTop() + TAB_HEIGHT + 8;
            int actionsTop = y + height - 20 * 2 - 6;
            scroll.place(x, listTop, width, actionsTop - listTop - 8);
            row(topRow, actionsTop);
            row(bottomRow, actionsTop + 26);
        }

        /** Buttons sharing a row's width equally, like Minecraft's own server list. */
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
            boolean editable = selected != null && selected != featured && !recentData.containsValue(selected);
            join.enabled(selected != null);
            edit.enabled(editable);
            delete.enabled(editable);
            layout();
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();

            canvas.push();
            canvas.scale(1.5f, x, y + 4);
            canvas.text(Theme.text(Component.literal("Multiplayer"), true), x, y + 4, colors.text(), shadow);
            canvas.pop();

            // Tabs: one highlight that slides to the open tab.
            int tabsTop = tabsTop();
            int trackWidth = TAB_WIDTH * Tab.values().length + 6;
            canvas.smoothRect(x, tabsTop, trackWidth, TAB_HEIGHT, 7f, colors.ghost());
            tabIndicator.set(tab.ordinal());
            int indicatorX = Math.round(x + 3 + tabIndicator.get() * TAB_WIDTH);
            canvas.smoothRect(indicatorX, tabsTop + 3, TAB_WIDTH, TAB_HEIGHT - 6, 4f, colors.ghostHovered());
            for (Tab each : Tab.values()) {
                String label = each == Tab.ALL ? each.label + "  " + (servers.size() + 1) : each.label;
                canvas.textCentered(Theme.text(Component.literal(label), each == tab),
                        x + 3 + each.ordinal() * TAB_WIDTH + TAB_WIDTH / 2, tabsTop + 7,
                        each == tab ? colors.text() : colors.textMuted(), shadow);
            }

            if (list.children().isEmpty()) {
                String empty = "Servers you join show up here.";
                canvas.textCentered(Theme.text(Component.literal(empty), false), x + width / 2, scroll.y() + 30,
                        colors.textMuted(), shadow);
            }
            super.render(canvas);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int tabsTop = tabsTop();
            if (button == 0 && mouseY >= tabsTop && mouseY < tabsTop + TAB_HEIGHT && mouseX >= x + 3
                    && mouseX < x + 3 + TAB_WIDTH * Tab.values().length) {
                showTab(Tab.values()[(int) ((mouseX - x - 3) / TAB_WIDTH)]);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private final class ServerCard extends Element {

        private final ServerData server;
        private final long appearAt;
        private final Tween hover = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
        private final Tween selection = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
        private long lastClick;

        ServerCard(ServerData server, int index) {
            this.server = server;
            this.appearAt = Clock.SYSTEM.nanos() + index * Motion.ROW_STAGGER_MS * 1_000_000L;
        }

        @Override
        public Size measure() {
            return new Size(300, CARD_HEIGHT);
        }

        /** Right edge of the status column. */
        private int statusRight() {
            return x + width - PADDING;
        }

        @Override
        public void render(Canvas canvas) {
            ping(server);
            float appear = Motion.ENTER.apply(Math.clamp((Clock.SYSTEM.nanos() - appearAt) / 240_000_000f, 0f, 1f));
            if (appear <= 0) {
                return;
            }
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            hover.set(contains(canvas.mouseX(), canvas.mouseY()) ? 1 : 0);
            selection.set(selected == server ? 1 : 0);
            float sel = selection.get();

            canvas.pushAlpha(appear);
            canvas.push();
            canvas.translate(0, (1 - appear) * 6);
            int fill = Lerp.color(Lerp.color(colors.ghost(), colors.ghostHovered(), hover.get()),
                    0x2E << 24 | (colors.primary() & 0xFFFFFF), sel);
            canvas.smoothRect(x, y, width, height, 8f, fill);
            canvas.smoothFrame(x, y, width, height, 8f, 1,
                    Lerp.color(colors.hairline(), colors.accentStrong(), sel));

            // Icon: inner radius = card radius - padding gap.
            int icon = 32;
            int iconX = x + 7;
            int iconY = y + (height - icon) / 2;
            Icon texture = icons.computeIfAbsent(server, Icon::new);
            if (texture.update(server.getIconBytes())) {
                canvas.image(texture.texture.textureLocation(), 64, iconX, iconY, icon);
            } else {
                canvas.smoothRect(iconX, iconY, icon, icon, 4f, colors.inset());
                canvas.textCentered(Theme.text(Component.literal(server.name.isEmpty() ? "?"
                        : server.name.substring(0, 1).toUpperCase()), true), iconX + icon / 2, iconY + 12,
                        colors.textMuted(), shadow);
            }

            int textX = iconX + icon + 9;
            canvas.text(Theme.text(Component.literal(server.name), true), textX, y + 9, colors.text(), shadow);
            int statusLeft = drawStatus(canvas, colors, shadow);
            // The MOTD in Minecraft's own font and at Minecraft's own line width (271), so a MOTD the server
            // centred with spaces for the vanilla list stays centred here.
            var lines = canvas.wrap(server.motd == null ? Component.empty() : server.motd, VANILLA_MOTD_WIDTH);
            if (!lines.isEmpty()) {
                canvas.line(lines.get(0), textX, y + 22, colors.textMuted(), true);
            }
            if (lines.size() > 1) {
                canvas.line(lines.get(1), textX, y + 32, colors.textMuted(), true);
            }

            canvas.pop();
            canvas.popAlpha();
        }

        /**
         * Ping (Minecraft's own signal icons, animated while pinging, like its server list) and players to the
         * left of the star; returns where the status starts.
         */
        private int drawStatus(Canvas canvas, Palette colors, boolean shadow) {
            int right = statusRight();
            Identifier sprite;
            String text;
            switch (server.state()) {
                case SUCCESSFUL -> {
                    long ping = server.ping;
                    int level = ping < 0 ? 0 : ping < 150 ? 5 : ping < 300 ? 4 : ping < 600 ? 3 : ping < 1000 ? 2 : 1;
                    sprite = level == 0 ? UNREACHABLE : PING[level - 1];
                    text = ping + " ms";
                }
                case INCOMPATIBLE -> {
                    sprite = INCOMPATIBLE;
                    text = "Incompatible";
                }
                case UNREACHABLE -> {
                    sprite = UNREACHABLE;
                    text = "Offline";
                }
                default -> {
                    // Minecraft's pinging animation: a light running up and down the bars.
                    int frame = (int) (System.currentTimeMillis() / 100L % 8);
                    sprite = PINGING[frame > 4 ? 8 - frame : frame];
                    text = "Pinging";
                }
            }
            canvas.sprite(sprite, right - 10, y + 9, 10, 8);
            Component status = Theme.text(Component.literal(text), false);
            canvas.textRight(status, right - 14, y + 9, colors.textMuted(), shadow);
            int left = right - 14 - canvas.textWidth(status);
            if (server.players != null && server.state() == ServerData.State.SUCCESSFUL) {
                Component players = Theme.text(Component.literal(server.players.online() + " / "
                        + server.players.max()), true);
                canvas.textRight(players, right, y + 26, colors.text(), shadow);
                left = Math.min(left, right - canvas.textWidth(players));
            }
            return left;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !contains(mouseX, mouseY)) {
                return false;
            }
            long now = System.currentTimeMillis();
            if (selected == server && now - lastClick < 300) {
                join(server);
            }
            selected = server;
            lastClick = now;
            return true;
        }
    }
}
