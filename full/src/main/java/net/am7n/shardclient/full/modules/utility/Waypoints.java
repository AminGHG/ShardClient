package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.platform.Chat;
import net.am7n.shardclient.shared.anim.Maths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.modules.Keys;
import net.am7n.shardclient.full.platform.Poses;
import net.am7n.shardclient.full.platform.Servers;
import net.am7n.shardclient.full.platform.Verts;
import net.am7n.shardclient.full.platform.World;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Waypoints extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FULL_BRIGHT = 0xE800F0;
    private static final int[] COLORS = {0xB27BFF, 0x45F0A0, 0x3D8BFF, 0xFFC233, 0xFF5FB4, 0x6FE3FF};
    private static final String DEATH = "Death";

    private final BooleanSetting beam = bool("beam", "Show beams", true);
    private final BooleanSetting showDistance = bool("distance", "Show distance", true);
    private final BooleanSetting deaths = bool("deaths", "Mark where you die", true);
    private final BooleanSetting throughWalls = bool("through_walls", "See through walls", true);
    private final NumberSetting size = number("size", "Label size", 1, 0.5, 3, 0.1, "×");

    private record Point(String name, int x, int y, int z, String dimension, int color) {
        Vec3 center() {
            return new Vec3(x + 0.5, y, z + 0.5);
        }
    }

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("shardclient").resolve("waypoints.json");
    private Map<String, List<Point>> all;
    private boolean wasDead;

    public Waypoints() {
        super("waypoints", "Waypoints",
                "Press B to mark a spot. Beams and distance show the way back.", Category.UTILITY);
        setEnabled(true);
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.level == null) {
            while (Keys.addWaypoint != null && Keys.addWaypoint.consumeClick()) {
            }
            while (Keys.removeWaypoint != null && Keys.removeWaypoint.consumeClick()) {
            }
            return;
        }
        while (Keys.addWaypoint != null && Keys.addWaypoint.consumeClick()) {
            Point point = add(nextName(), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            say("Added " + point.name() + " at " + point.x() + " " + point.y() + " " + point.z());
        }
        while (Keys.removeWaypoint != null && Keys.removeWaypoint.consumeClick()) {
            Point looked = lookedAt();
            if (looked == null) {
                say("Look at a waypoint to remove it");
            } else {
                points().remove(looked);
                save();
                say("Removed " + looked.name());
            }
        }
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead && deaths.on()) {
            points().removeIf(p -> p.name().equals(DEATH) && p.dimension().equals(dimension()));
            add(DEATH, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        }
        wasDead = dead;
    }

    private void say(String text) {
        if (mc.player != null) {
            Chat.actionBar(Component.literal(text));
        }
    }

    private Point add(String name, int x, int y, int z) {
        List<Point> list = points();
        int color = name.equals(DEATH) ? 0xFF5555 : COLORS[Math.floorMod(list.size(), COLORS.length)];
        Point point = new Point(name, x, y, z, dimension(), color);
        list.add(point);
        save();
        return point;
    }

    private String nextName() {
        int highest = 0;
        for (Point point : points()) {
            if (point.name().startsWith("Waypoint ")) {
                try {
                    highest = Math.max(highest, Integer.parseInt(point.name().substring(9)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "Waypoint " + (highest + 1);
    }

    private Point lookedAt() {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Point best = null;
        double bestAngle = Math.toRadians(8);
        for (Point point : points()) {
            if (!point.dimension().equals(dimension())) {
                continue;
            }
            Vec3 to = point.center().add(0, 1, 0).subtract(eye);
            if (to.lengthSqr() < 1.0e-4) {
                return point;
            }
            double angle = Math.acos(Maths.clamp(to.normalize().dot(look), -1, 1));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = point;
            }
        }
        return best;
    }

    private String dimension() {
        return Servers.dimension();
    }

    private String context() {
        if (mc.getSingleplayerServer() != null) {
            return "world:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData server = mc.getCurrentServer();
        return server != null ? "server:" + server.ip.toLowerCase(Locale.ROOT) : "other";
    }

    private List<Point> points() {
        if (all == null) {
            load();
        }
        return all.computeIfAbsent(context(), key -> new ArrayList<>());
    }

    public void render(World world) {
        if (!isEnabled() || mc.player == null || mc.level == null) {
            return;
        }
        String dimension = dimension();
        List<Point> here = new ArrayList<>();
        for (Point point : points()) {
            if (point.dimension().equals(dimension)) {
                here.add(point);
            }
        }
        if (here.isEmpty()) {
            return;
        }
        World.View view = world.view();
        if (view == null) {
            return;
        }
        Vec3 cam = view.position();
        PoseStack pose = world.pose();
        Font font = mc.font;
        double maxShown = Math.max(24, mc.options.getEffectiveRenderDistance() * 16 * 0.7);

        for (Point point : here) {
            Vec3 delta = point.center().subtract(cam);
            double distance = delta.length();
            double shown = Math.min(distance, maxShown);
            double k = distance < 1.0e-3 ? 1 : shown / distance;
            Vec3 at = delta.scale(k);

            if (beam.on()) {
                double half = Math.max(0.12, distance * 0.002) * k;
                double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                Vec3 side = horizontal < 1.0e-4 ? new Vec3(half, 0, 0)
                        : new Vec3(-delta.z / horizontal * half, 0, delta.x / horizontal * half);
                float height = (float) (220 * k);
                int rgb = point.color();
                world.flat(pose, out -> beam(out, at, side, height, rgb));
            }

            pose.pushPose();
            pose.translate(at.x, at.y + 1.6 * k, at.z);
            Poses.rotate(pose, view.rotation());
            float scale = 0.025f * size.floatValue() * (float) Math.max(1, shown / 8.0);
            pose.scale(scale, -scale, scale);
            Component label = label(point, distance);
            float x = -font.width(label) / 2f;
            world.text(pose, x, 0, label, false, throughWalls.on(), FULL_BRIGHT,
                    0xFFFFFFFF, 0x66000000, 0);
            pose.popPose();
        }
    }

    private Component label(Point point, double distance) {
        MutableComponent label = Component.literal("◆ ").withStyle(Style.EMPTY.withColor(point.color()))
                .append(Component.literal(point.name()).withStyle(Style.EMPTY.withColor(0xFFFFFF)));
        if (showDistance.on()) {
            label.append(Component.literal("  " + Math.round(distance) + "m").withStyle(Style.EMPTY.withColor(0xB8B8C8)));
        }
        return label;
    }

    private static void beam(Verts out, Vec3 at, Vec3 side, float height, int rgb) {
        int bottom = 0x88 << 24 | rgb;
        int top = rgb;
        float x0 = (float) (at.x - side.x), z0 = (float) (at.z - side.z);
        float x1 = (float) (at.x + side.x), z1 = (float) (at.z + side.z);
        float y0 = (float) at.y, y1 = (float) at.y + height;
        out.color(x0, y0, z0, bottom, FULL_BRIGHT);
        out.color(x1, y0, z1, bottom, FULL_BRIGHT);
        out.color(x1, y1, z1, top, FULL_BRIGHT);
        out.color(x0, y1, z0, top, FULL_BRIGHT);
        out.color(x0, y1, z0, top, FULL_BRIGHT);
        out.color(x1, y1, z1, top, FULL_BRIGHT);
        out.color(x1, y0, z1, bottom, FULL_BRIGHT);
        out.color(x0, y0, z0, bottom, FULL_BRIGHT);
    }

    private void load() {
        all = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            if (!(root instanceof JsonObject contexts)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : contexts.entrySet()) {
                List<Point> list = new ArrayList<>();
                if (entry.getValue() instanceof JsonArray array) {
                    for (JsonElement element : array) {
                        if (element instanceof JsonObject o) {
                            list.add(new Point(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
                                    o.get("z").getAsInt(), o.get("dimension").getAsString(), o.get("color").getAsInt()));
                        }
                    }
                }
                all.put(entry.getKey(), list);
            }
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.warn("Couldn't read waypoints", ex);
        }
    }

    private void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, List<Point>> entry : all.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            JsonArray array = new JsonArray();
            for (Point point : entry.getValue()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", point.name());
                o.addProperty("x", point.x());
                o.addProperty("y", point.y());
                o.addProperty("z", point.z());
                o.addProperty("dimension", point.dimension());
                o.addProperty("color", point.color());
                array.add(o);
            }
            root.add(entry.getKey(), array);
        }
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("waypoints.json.tmp");
            Files.writeString(temp, GSON.toJson(root));
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.warn("Couldn't save waypoints", ex);
        }
    }
}
