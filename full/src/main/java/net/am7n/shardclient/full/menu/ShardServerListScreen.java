package net.am7n.shardclient.full.menu;

import com.mojang.blaze3d.platform.NativeImage;
import net.am7n.shardclient.shared.ShardClientRuntime;
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
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ShardClient's server list: the same servers, pings and add / edit / delete / direct connect as Minecraft's
 * (it uses Minecraft's own list, pinger and dialogs), shown as cards in the client's style, with ShardPvP
 * pinned on top.
 */
public final class ShardServerListScreen extends ShardScreen {

    private static final int CARD_HEIGHT = 44;
    private static final int PADDING = 10;

    private final Screen parent;
    private final ServerList servers;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final ServerData featured = new ServerData("ShardPvP", ShardTitleScreen.SHARDPVP_ADDRESS, ServerData.Type.OTHER);
    private final Map<ServerData, Icon> icons = new HashMap<>();
    private View view;

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
        int width = Math.min(screenWidth - 24, 420);
        root.place((screenWidth - width) / 2, 12, width, screenHeight - 24);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            extractPanorama(graphics, partialTick);
        }
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        int bg = Theme.colors().bg0() & 0xFFFFFF;
        canvas.pushAlpha(openProgress());
        canvas.gradient(0, 0, width, height, 0x88 << 24 | bg, 0xD8 << 24 | bg);
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

    /** Back here after a dialog, with the list rebuilt from the saved servers. */
    private void returnHere() {
        servers.load();
        minecraft.gui.setScreen(this);
        if (view != null) {
            view.rebuild();
        }
    }

    // Actions

    private void join(ServerData server) {
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

    private void edit(ServerData server) {
        minecraft.gui.setScreen(new ManageServerScreen(this, Component.literal("Edit Server Info"), accepted -> {
            if (accepted) {
                servers.save();
            } else {
                servers.load();
            }
            returnHere();
        }, server));
    }

    private void delete(ServerData server) {
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

        private final Stack list = Stack.column(4);
        private final ScrollArea scroll = new ScrollArea(list, 100);
        private final List<Button> actions = List.of(
                new Button("Add server", ShardServerListScreen.this::add),
                new Button("Direct connect", ShardServerListScreen.this::directConnect),
                new Button("Refresh", ShardServerListScreen.this::refresh));
        private final Button back = new Button("Back", ShardServerListScreen.this::onClose);

        View() {
            add(scroll);
            actions.forEach(this::add);
            add(back);
            rebuild();
        }

        void rebuild() {
            list.children().clear();
            List<ServerData> entries = new ArrayList<>();
            boolean listed = false;
            for (int i = 0; i < servers.size(); i++) {
                ServerData server = servers.get(i);
                listed |= server.ip.equalsIgnoreCase(featured.ip);
                entries.add(server);
            }
            int index = 0;
            if (!listed) {
                list.add(new ServerCard(featured, true, index++));
            }
            for (ServerData server : entries) {
                list.add(new ServerCard(server, server.ip.equalsIgnoreCase(featured.ip), index++));
            }
            scroll.scrollToTop();
            layout();
        }

        @Override
        public Size measure() {
            return new Size(width, height);
        }

        @Override
        protected void layout() {
            int top = y + 34;
            scroll.place(x, top, width, height - (top - y) - 30);
            int ax = x + width;
            for (int i = actions.size() - 1; i >= 0; i--) {
                Size size = actions.get(i).measure();
                ax -= size.width();
                actions.get(i).place(ax, y + 4, size.width(), size.height());
                ax -= 4;
            }
            Size backSize = back.measure();
            back.place(x + (width - 120) / 2, y + height - backSize.height(), 120, backSize.height());
        }

        @Override
        public void render(Canvas canvas) {
            layout();
            Look look = Theme.look();
            canvas.push();
            canvas.scale(1.5f, x, y + 8);
            canvas.text(Theme.text(Component.literal("Multiplayer"), true), x, y + 8, look.colors().text(),
                    look.textShadow());
            canvas.pop();
            super.render(canvas);
        }
    }

    private final class ServerCard extends Container {

        private final ServerData server;
        private final boolean featuredCard;
        private final long appearAt;
        private final Button join;
        private final Button edit;
        private final Button delete;
        private long lastClick;

        ServerCard(ServerData server, boolean featuredCard, int index) {
            this.server = server;
            this.featuredCard = featuredCard;
            this.appearAt = System.nanoTime() + index * 30_000_000L;
            this.join = new Button("Join", () -> join(server)).style(Button.Style.PRIMARY);
            this.edit = new Button("Edit", () -> edit(server));
            this.delete = new Button("Remove", () -> delete(server));
            add(join);
            if (server != featured) {
                add(edit);
                add(delete);
            }
        }

        @Override
        public Size measure() {
            return new Size(300, CARD_HEIGHT);
        }

        @Override
        protected void layout() {
            int bx = x + width - PADDING;
            for (Element button : new Element[]{delete, edit, join}) {
                if (!children.contains(button)) {
                    continue;
                }
                Size size = button.measure();
                bx -= size.width();
                button.place(bx, y + (height - 18) / 2, size.width(), 18);
                bx -= 4;
            }
        }

        private boolean hovered(Canvas canvas) {
            return contains(canvas.mouseX(), canvas.mouseY());
        }

        @Override
        public void render(Canvas canvas) {
            ping(server);
            float appear = Math.clamp((System.nanoTime() - appearAt) / 240_000_000f, 0f, 1f);
            if (appear <= 0) {
                return;
            }
            appear = net.am7n.shardclient.shared.anim.Motion.ENTER.apply(appear);
            Look look = Theme.look();
            Palette colors = look.colors();
            boolean shadow = look.textShadow();
            boolean hovered = hovered(canvas);

            canvas.pushAlpha(appear);
            canvas.push();
            canvas.translate(0, (1 - appear) * 6);
            canvas.smoothRect(x, y, width, height, 7f, hovered ? colors.ghostHovered() : colors.ghost());
            canvas.smoothFrame(x, y, width, height, 7f, 1,
                    featuredCard ? (0x88 << 24 | (colors.accentStrong() & 0xFFFFFF)) : colors.hairline());

            int icon = 32;
            int iconX = x + PADDING;
            int iconY = y + (height - icon) / 2;
            Icon texture = icons.computeIfAbsent(server, Icon::new);
            if (texture.update(server.getIconBytes())) {
                canvas.image(texture.texture.textureLocation(), 64, iconX, iconY, icon);
            } else {
                canvas.smoothRect(iconX, iconY, icon, icon, 6f, colors.inset());
                canvas.textCentered(Theme.text(Component.literal(server.name.isEmpty() ? "?"
                        : server.name.substring(0, 1).toUpperCase()), true), iconX + icon / 2, iconY + 12,
                        colors.textMuted(), shadow);
            }

            int textX = iconX + icon + 8;
            String name = server.name + (featuredCard ? "  ·  Featured" : "");
            canvas.text(Theme.text(Component.literal(name), true), textX, y + 8, colors.text(), shadow);
            int maxMotd = (hovered ? join.x() - 8 : x + width - PADDING - 60) - textX;
            var lines = canvas.wrap(Theme.text(server.motd == null ? Component.empty() : server.motd, false), maxMotd);
            int lineY = y + 20;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                canvas.line(lines.get(i), textX, lineY, colors.textMuted(), shadow);
                lineY += 10;
            }

            if (hovered) {
                for (Element child : children) {
                    child.render(canvas);
                }
            } else {
                drawStatus(canvas, colors, shadow);
            }
            canvas.pop();
            canvas.popAlpha();
        }

        /** Ping as signal bars plus milliseconds, and the player count, on the right. */
        private void drawStatus(Canvas canvas, Palette colors, boolean shadow) {
            int right = x + width - PADDING;
            String text;
            int bars;
            int color;
            switch (server.state()) {
                case SUCCESSFUL -> {
                    long ping = server.ping;
                    bars = ping < 80 ? 4 : ping < 150 ? 3 : ping < 300 ? 2 : 1;
                    color = bars >= 3 ? colors.positive() : bars == 2 ? colors.warning() : colors.negative();
                    text = ping + " ms";
                }
                case INCOMPATIBLE -> {
                    bars = 0;
                    color = colors.negative();
                    text = "Incompatible";
                }
                case UNREACHABLE -> {
                    bars = 0;
                    color = colors.negative();
                    text = "Offline";
                }
                default -> {
                    bars = 0;
                    color = colors.textMuted();
                    text = "Pinging…";
                }
            }
            for (int i = 0; i < 4; i++) {
                int barHeight = 3 + i * 2;
                canvas.smoothRect(right - (4 - i) * 4, y + 16 - barHeight, 3, barHeight, 1f,
                        i < bars ? color : colors.ghostHovered());
            }
            canvas.textRight(Theme.text(Component.literal(text), false), right - 20, y + 8, colors.textMuted(), shadow);
            if (server.players != null && server.state() == ServerData.State.SUCCESSFUL) {
                String players = server.players.online() + " / " + server.players.max();
                canvas.textRight(Theme.text(Component.literal(players), true), right, y + 26, colors.text(), shadow);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            long now = System.currentTimeMillis();
            if (button == 0 && now - lastClick < 300) {
                join(server);
            }
            lastClick = now;
            return true;
        }
    }
}
