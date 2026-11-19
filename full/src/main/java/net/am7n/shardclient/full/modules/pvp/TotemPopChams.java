package net.am7n.shardclient.full.modules.pvp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.mixin.module.ModelPartAccessor;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.platform.PlayerPose;
import net.am7n.shardclient.full.platform.Poses;
import net.am7n.shardclient.full.platform.Verts;
import net.am7n.shardclient.full.platform.World;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TotemPopChams extends Module {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float FADE_START = 0.8f;
    private static final String[] COLOURS = {"Purple", "Violet", "Lilac", "Pink", "Red", "Blue", "Mint", "Gold",
            "White"};

    private final BooleanSetting ownPops = bool("own_pops", "Show own pops", false);
    private final NumberSetting lifetime = number("lifetime", "Lifetime", 50, 1, 250, 1, " ticks");
    private final BooleanSetting fadeOut = bool("fade_out", "Fade out", true);
    private final BooleanSetting disperse = bool("disperse", "Disperse", false);
    private final NumberSetting disperseSpeed = number("disperse_speed", "Disperse speed", 1.5, 0, 10, 0.5, "");
    private final NumberSetting disperseDistance = number("disperse_distance", "Disperse distance", 1, 0, 8, 0.1,
            " blocks");
    private final BooleanSetting fill = bool("fill", "Filled model", true);
    private final ChoiceSetting fillColour = choice("fill_colour", "Fill colour", "Purple", COLOURS);
    private final BooleanSetting wireframe = bool("wireframe", "Wireframe", true);
    private final ChoiceSetting wireColour = choice("wire_colour", "Wireframe colour", "Violet", COLOURS);
    private final NumberSetting wireThickness = number("wire_thickness", "Wireframe thickness", 2, 0, 5, 0.1, "");
    private final NumberSetting opacity = number("opacity", "Opacity", 59, 5, 100, 1, "%");

    private final List<Ghost> ghosts = new ArrayList<>();
    private ClientLevel level;

    public TotemPopChams() {
        super("totem_pop_chams", "Totem pop chams",
                "Leaves a fading ghost of a player where they pop a totem of undying.", Category.PVP);
        disperseSpeed.showIf(disperse::on);
        disperseDistance.showIf(disperse::on);
        fillColour.showIf(fill::on);
        wireColour.showIf(wireframe::on);
        wireThickness.showIf(wireframe::on);
    }

    @Override
    protected void onDisable() {
        ghosts.clear();
    }

    @Override
    public void tick() {
        if (mc.level != level) {
            level = mc.level;
            ghosts.clear();
        }
        if (mc.isPaused()) {
            return;
        }
        int life = lifetime.intValue();
        ghosts.removeIf(ghost -> ++ghost.age >= life);
    }

    public void onPop(Player player) {
        if (!isEnabled() || mc.level == null || (player == mc.player && !ownPops.on())) {
            return;
        }
        try {
            Ghost ghost = capture(player);
            if (ghost != null) {
                ghosts.add(ghost);
            }
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.warn("Could not capture totem pop of {}", player.getName().getString(), ex);
        }
    }

    private Ghost capture(Player player) {
        PlayerPose pose = PlayerPose.capture(player);
        return pose == null ? null : new Ghost(player.position(), player.yBodyRot, pose);
    }

    public void render(World world) {
        if (!isEnabled() || ghosts.isEmpty()) {
            return;
        }
        World.View view = world.view();
        if (view == null) {
            return;
        }
        Vec3 cam = view.position();
        PoseStack pose = world.pose();
        float partial = world.partialTick();
        int fillArgb = argb(fillColour.get());
        int wireArgb = argb(wireColour.get());
        float thickness = wireThickness.floatValue();
        for (Ghost ghost : List.copyOf(ghosts)) {
            float spread = displacement(ghost, partial);
            float alpha = alpha(ghost, partial, spread);
            if (fill.on()) {
                int colour = fade(fillArgb, alpha);
                place(pose, ghost, cam);
                if (!disperse.on()) {
                    world.chamsFill(pose, out -> ghost.pose.render(out, FULL_BRIGHT, colour));
                } else {
                    for (BodyPart part : BodyPart.values()) {
                        Vec3 offset = offset(ghost, part, spread);
                        pose.pushPose();
                        pose.translate(offset.x, offset.y, offset.z);
                        ModelPart modelPart = part.of(ghost.pose);
                        world.chamsFill(pose, out -> out.part(modelPart, FULL_BRIGHT, colour));
                        pose.popPose();
                    }
                }
                pose.popPose();
            }
            if (wireframe.on()) {
                int colour = fade(wireArgb, alpha);
                place(pose, ghost, cam);
                for (BodyPart part : BodyPart.values()) {
                    Vec3 offset = offset(ghost, part, spread);
                    pose.pushPose();
                    pose.translate(offset.x, offset.y, offset.z);
                    ModelPart modelPart = part.of(ghost.pose);
                    world.chamsWire(pose, thickness, out -> wire(modelPart, out, colour, thickness));
                    pose.popPose();
                }
                pose.popPose();
            }
        }
    }

    private float displacement(Ghost ghost, float partial) {
        if (!disperse.on()) {
            return 0;
        }
        float time = Math.max(0, ghost.age - 1 + partial);
        float speed = (float) Math.pow(Mth.clamp(disperseSpeed.get(), 1, 10) / 10.0, 2) * 2f;
        return (float) Math.min(time * speed, disperseDistance.get());
    }

    private float alpha(Ghost ghost, float partial, float spread) {
        if (!fadeOut.on()) {
            return 1;
        }
        float time = Math.max(0, ghost.age - 1 + partial);
        float life = lifetime.floatValue();
        float fadeFrom = life * FADE_START;
        float alpha = time >= fadeFrom ? 1f - (time - fadeFrom) / Math.max(1e-3f, life - fadeFrom) : 1f;
        if (disperse.on() && disperseDistance.get() > 0) {
            alpha *= (float) (1 - spread / disperseDistance.get());
        }
        return Mth.clamp(alpha, 0, 1);
    }

    private static Vec3 offset(Ghost ghost, BodyPart part, float spread) {
        if (spread <= 0) {
            return Vec3.ZERO;
        }
        RandomSource random = RandomSource.create(ghost.seed.getMostSignificantBits()
                ^ ghost.seed.getLeastSignificantBits() ^ part.ordinal());
        Vec3 noise = new Vec3((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5,
                (random.nextDouble() - 0.5) * 0.5);
        return part.direction.add(noise).normalize().scale(spread);
    }

    private static void place(PoseStack pose, Ghost ghost, Vec3 cam) {
        pose.pushPose();
        pose.translate(ghost.position.x - cam.x, ghost.position.y - cam.y, ghost.position.z - cam.z);
        Poses.rotate(pose, new Quaternionf().rotationY((float) Math.toRadians(180 - ghost.bodyYaw)));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.scale(0.8f, 0.9f, 0.8f);
        pose.translate(0.0F, -1.5F, 0.0F);
        if (!ghost.pose.autoSpinAttack) {
            Poses.rotate(pose, new Quaternionf().rotationX(
                    (float) Math.toRadians(ghost.pose.glidingProcess * (90.0F - ghost.pose.pitch))));
        }
        if (ghost.pose.applyFlyingYRot) {
            Poses.rotate(pose, new Quaternionf().rotationY(ghost.pose.flyingRotation));
        }
    }

    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private static void wire(ModelPart part, Verts out, int argb, float width) {
        PoseStack stack = out.stack();
        stack.pushPose();
        part.translateAndRotate(stack);
        for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).shardclient$cubes()) {
            float x1 = cube.minX / 16f, y1 = cube.minY / 16f, z1 = cube.minZ / 16f;
            float x2 = cube.maxX / 16f, y2 = cube.maxY / 16f, z2 = cube.maxZ / 16f;
            Vector3f[] corners = {
                    new Vector3f(x1, y1, z1), new Vector3f(x2, y1, z1), new Vector3f(x2, y2, z1),
                    new Vector3f(x1, y2, z1), new Vector3f(x1, y1, z2), new Vector3f(x2, y1, z2),
                    new Vector3f(x2, y2, z2), new Vector3f(x1, y2, z2)
            };
            for (int[] edge : EDGES) {
                Vector3f from = corners[edge[0]];
                Vector3f to = corners[edge[1]];
                Vector3f normal = new Vector3f(to).sub(from).normalize();
                out.line(from.x, from.y, from.z, argb, width, normal.x, normal.y, normal.z);
                out.line(to.x, to.y, to.z, argb, width, normal.x, normal.y, normal.z);
            }
        }
        stack.popPose();
    }

    private int argb(String colour) {
        int rgb = switch (colour) {
            case "Violet" -> 0x6400FF;
            case "Lilac" -> 0xB27BFF;
            case "Pink" -> 0xFF5FB4;
            case "Red" -> 0xFF3B3B;
            case "Blue" -> 0x3D8BFF;
            case "Mint" -> 0x45F0A0;
            case "Gold" -> 0xFFC233;
            case "White" -> 0xFFFFFF;
            default -> 0x8900FF;
        };
        int alpha = Math.round(opacity.floatValue() / 100f * 255);
        return alpha << 24 | rgb;
    }

    private static int fade(int argb, float alpha) {
        int a = Mth.clamp(Math.round(((argb >>> 24) & 0xFF) * alpha), 0, 255);
        return a << 24 | (argb & 0xFFFFFF);
    }

    private enum BodyPart {
        HEAD(new Vec3(0, -1, 0)),
        BODY(Vec3.ZERO),
        LEFT_ARM(new Vec3(1, 0, 0)),
        RIGHT_ARM(new Vec3(-1, 0, 0)),
        LEFT_LEG(new Vec3(0.3, 1, 0)),
        RIGHT_LEG(new Vec3(-0.3, 1, 0));

        final Vec3 direction;

        BodyPart(Vec3 direction) {
            this.direction = direction;
        }

        ModelPart of(PlayerPose pose) {
            return pose.parts()[ordinal()];
        }
    }

    private static final class Ghost {
        final Vec3 position;
        final float bodyYaw;
        final PlayerPose pose;
        final UUID seed = UUID.randomUUID();
        int age;

        Ghost(Vec3 position, float bodyYaw, PlayerPose pose) {
            this.position = position;
            this.bodyYaw = bodyYaw;
            this.pose = pose;
        }
    }
}
