package net.am7n.shardclient.full.modules.lyrics;

import net.am7n.shardclient.shared.anim.Maths;
import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.media.MediaWatcher;
import net.am7n.shardclient.full.platform.Poses;
import net.am7n.shardclient.full.platform.Verts;
import net.am7n.shardclient.full.platform.World;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorldLyrics {
    private static final int FULL_BRIGHT = 0xE800F0;

    private static final float BASE_SCALE = 0.075f;

    private final LyricsModule module;
    private final Map<Integer, Spot> spots = new HashMap<>();
    private String trackKey = "";

    private record Spot(Vec3 position, Vec3 toward, float tilt, float scale) {
        Quaternionf facing(Vec3 camera) {
            float yaw = (float) Math.atan2(toward.x, -toward.z);
            Quaternionf q = new Quaternionf().rotationYXZ((float) Math.PI - yaw, tilt, 0);
            Vec3 now = camera.subtract(position);
            if (now.x * toward.x + now.z * toward.z < 0) {
                q.rotateY((float) Math.PI);
            }
            return q;
        }
    }

    private final java.util.Random random = new java.util.Random();
    private boolean nextOnRight;

    public WorldLyrics(LyricsModule module) {
        this.module = module;
    }

    public void register() {
        World.onRender(this::render);
    }

    private void render(World world) {
        if (!module.isEnabled() || !module.inWorld()) {
            spots.clear();
            return;
        }
        try {
            draw(world);
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.error("World lyrics failed and were switched to the screen", ex);
            module.place.set("Screen");
        }
    }

    private void draw(World world) {
        Minecraft mc = Minecraft.getInstance();
        World.View view = world.view();
        if (mc.player == null || view == null) {
            return;
        }

        String key = MediaWatcher.get().playing().map(t -> t.artist() + "/" + t.title()).orElse("")
                + "|" + module.place.get();
        if (!key.equals(trackKey)) {
            trackKey = key;
            spots.clear();
        }
        List<LyricFrame> frames = module.frames();
        if (frames.isEmpty()) {
            return;
        }
        Vec3 cam = view.position();
        PoseStack pose = world.pose();
        Font font = mc.font;

        int currentId = frames.stream().filter(f -> f.role() == LyricFrame.Role.CURRENT)
                .mapToInt(LyricFrame::lineId).findFirst().orElse(-1);
        float currentHeight = LyricLayout.ROW;
        for (LyricFrame frame : frames) {
            if (frame.role() == LyricFrame.Role.CURRENT) {
                currentHeight = LyricLayout.height(rows(font, frame));
            }
        }
        for (LyricFrame frame : frames) {
            boolean next = frame.role() == LyricFrame.Role.NEXT;
            List<LyricLayout.Row> rows = rows(font, frame);
            int spotId = next ? currentId : frame.lineId();
            int widthPx = Math.max(1, LyricLayout.width(rows));
            Spot spot = module.place.is("Follow view") ? place(view, spotId, widthPx)
                    : spots.computeIfAbsent(spotId, id -> place(view, id, widthPx));
            if (spot == null) {
                continue;
            }
            int alpha = Math.round(Maths.clamp(frame.alpha(), 0f, 1f) * 255);
            if (alpha <= 3) {
                continue;
            }
            pose.pushPose();
            pose.translate(spot.position().x - cam.x, spot.position().y - cam.y, spot.position().z - cam.z);
            Poses.rotate(pose, module.facePlayer.on() || module.place.is("Follow view") ? view.rotation() : spot.facing(cam));
            float scale = spot.scale() * frame.scale() * (next ? 0.75f : 1f);
            pose.scale(scale, -scale, scale);

            float blockHeight = LyricLayout.height(rows);
            float shift = next ? (currentHeight - LyricLayout.ROW) / 2f / 0.75f : -(blockHeight - LyricLayout.ROW) / 2f;
            float rowY = frame.offsetY() / (next ? 0.75f : 1f) - 4 + shift;
            boolean solid = solid(frame);
            boolean bold = module.bold.on();
            float depth = module.depth.floatValue();
            float glow = next ? 0 : module.glowStrength();
            int[] glowColors = module.glowColors(frame.color());

            for (LyricLayout.Row row : rows) {
                Component part = LyricLayout.slice(frame.text(), row.start(), row.end());
                float x = -row.width() / 2f;
                pose.pushPose();
                pose.translate(0, rowY, 0);
                pose.scale(row.scale(), row.scale(), row.scale());
                if (glow > 0) {
                    int shown = Math.min(row.width(),
                            solid ? VoxelFont.width(LyricLayout.plain(part), bold) : font.width(part));
                    LyricGlow.Texture texture = shown > 0 ? module.glowTexture(frame, row) : null;
                    if (texture != null) {
                        float u1 = (shown + LyricGlow.PAD * 2f) / (row.width() + LyricGlow.PAD * 2f);
                        float x0 = x - LyricGlow.PAD;
                        float x1 = x + shown + LyricGlow.PAD;
                        float z = -(solid ? depth : 0) - 0.3f;
                        int glowAlpha = Math.round(Math.min(1f, glow) * alpha) << 24;
                        int left = glowAlpha | glowColors[0] & 0xFFFFFF;
                        int right = glowAlpha | LyricsModule.lerp(glowColors[0], glowColors[1], u1) & 0xFFFFFF;
                        world.glow(pose, texture.id(), module.throughWalls.on(),
                                out -> glowQuad(out, x0, x1, z, u1, left, right));
                    }
                }
                if (solid) {
                    StringBuilder plain = new StringBuilder();
                    List<Integer> colors = new java.util.ArrayList<>();
                    part.visit((style, text) -> {
                        int rgb = style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
                        text.codePoints().forEach(cp -> {
                            plain.appendCodePoint(cp);
                            colors.add(alpha << 24 | rgb);
                        });
                        return java.util.Optional.empty();
                    }, net.minecraft.network.chat.Style.EMPTY);
                    if (!colors.isEmpty()) {
                        String line = plain.toString();
                        int[] perChar = colors.stream().mapToInt(Integer::intValue).toArray();
                        world.flat(pose,
                                out -> VoxelFont.draw(out, line, perChar, x, 0, depth, bold,
                                        FULL_BRIGHT));
                    }
                } else {
                    int outline = module.outline.on() ? alpha << 24 : 0;
                    world.text(pose, x, 0, part,
                            module.shadow.on() && !module.outline.on(), module.throughWalls.on(), FULL_BRIGHT, alpha << 24 | 0xFFFFFF, 0,
                            outline);
                }
                pose.popPose();
                rowY += row.height();
            }
            pose.popPose();
        }

        spots.keySet().removeIf(id -> Math.abs(id - currentId) > 2);
    }

    private static void glowQuad(Verts out, float x0, float x1, float z, float u1,
                                 int left, int right) {
        float y0 = -LyricGlow.PAD;
        float y1 = y0 + LyricGlow.QUAD_HEIGHT;
        out.textured(x0, y0, z, left, 0, 0, FULL_BRIGHT);
        out.textured(x0, y1, z, left, 0, 1, FULL_BRIGHT);
        out.textured(x1, y1, z, right, u1, 1, FULL_BRIGHT);
        out.textured(x1, y0, z, right, u1, 0, FULL_BRIGHT);
        out.textured(x1, y0, z, right, u1, 0, FULL_BRIGHT);
        out.textured(x1, y1, z, right, u1, 1, FULL_BRIGHT);
        out.textured(x0, y1, z, left, 0, 1, FULL_BRIGHT);
        out.textured(x0, y0, z, left, 0, 0, FULL_BRIGHT);
    }

    private boolean solid(LyricFrame frame) {
        return module.letters.is("3D") && module.pixelFont(frame.lineId());
    }

    private List<LyricLayout.Row> rows(Font font, LyricFrame frame) {
        boolean words = module.wordsLayout() && frame.role() != LyricFrame.Role.NEXT;
        boolean bold = module.bold.on();
        java.util.function.ToIntFunction<Component> measure = solid(frame)
                ? c -> VoxelFont.width(LyricLayout.plain(c), bold)
                : font::width;
        String style = (solid(frame) ? "voxel" : module.fontFor(frame.lineId())) + bold;
        return LyricLayout.rows(frame.full(), words, module.maxLineWidth(), style, measure);
    }

    private Spot place(World.View view, int id, int widthPx) {
        Vec3 cam = view.position();
        Vec3 look = view.forward();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        double distance = module.distance.get();
        double height = module.height.get();
        float size = module.worldSize.floatValue();
        float sizeBoost = 1f;

        double spread = module.sideSpread.get();
        nextOnRight = !nextOnRight;
        double sideAngle = spread <= 0 ? 0 : (nextOnRight ? 1 : -1) * (spread * (0.7 + random.nextDouble() * 0.6));
        Vec3 target = switch (module.place.get()) {
            case "Sky" -> {
                double far = module.skyDistance.get();
                sizeBoost = 1.5f;
                Vec3 dir = turn(flat, sideAngle * 1.6);
                yield cam.add(dir.scale(far)).add(0, module.skyHeight.get() + height, 0);
            }
            case "Around you" -> {
                double angle = (nextOnRight ? 1 : -1) * (35 + random.nextDouble() * 25);
                yield cam.add(turn(flat, angle).scale(distance)).add(0, height, 0);
            }
            case "Follow view" -> cam.add(look.scale(distance)).add(0, height * 0.3, 0);

            default -> cam.add(turn(flat, sideAngle).scale(distance)).add(0, height, 0);
        };
        float scale = BASE_SCALE * sizeBoost * size * (float) (target.distanceTo(cam) / 7);
        Vec3 toward = new Vec3(cam.x - target.x, 0, cam.z - target.z);
        toward = toward.lengthSqr() < 1.0e-6 ? flat.scale(-1) : toward.normalize();

        Vec3 at = target;
        if (!module.place.is("Follow view")) {
            Vec3 right = new Vec3(toward.z, 0, -toward.x);
            double halfWidth = widthPx / 2.0 * scale;
            Vec3 lifted = lift(at, right, halfWidth, scale);
            if (lifted != null) {
                at = lifted;
            } else if (module.place.is("Sky")) {
                Vec3 higher = lift(at.add(0, 8, 0), right, halfWidth, scale);
                at = higher != null ? higher : at.add(0, 16, 0);
            } else {
                Vec3 nearer = clearOfBlocks(cam, target);
                Vec3 nearerLifted = lift(nearer, right, halfWidth, scale);
                at = nearerLifted != null ? nearerLifted : nearer;
                scale = BASE_SCALE * sizeBoost * size * (float) (at.distanceTo(cam) / 7);
            }
        }

        Vec3 toSpot = at.subtract(cam);
        double horizontal = Math.sqrt(toSpot.x * toSpot.x + toSpot.z * toSpot.z);
        float tilt = module.place.is("Sky") ? (float) Math.atan2(toSpot.y, Math.max(0.01, horizontal)) : 0f;
        return new Spot(at, toward, tilt, scale);
    }

    private static Vec3 lift(Vec3 at, Vec3 right, double halfWidth, float scale) {
        Vec3 point = at;
        for (int step = 0; step <= 16; step++) {
            if (free(point, right, halfWidth, scale)) {
                return point;
            }
            point = point.add(0, 0.5, 0);
        }
        return null;
    }

    private static boolean free(Vec3 at, Vec3 right, double halfWidth, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return true;
        }
        double top = 6 * scale;
        double bottom = -16 * scale;
        for (int i = 0; i <= 6; i++) {
            double across = -halfWidth + 2 * halfWidth * i / 6.0;
            for (double up : new double[]{top, (top + bottom) / 2, bottom}) {
                Vec3 sample = at.add(right.scale(across)).add(0, up, 0);
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(net.minecraft.util.Mth.floor(sample.x),
                        net.minecraft.util.Mth.floor(sample.y), net.minecraft.util.Mth.floor(sample.z));
                var state = mc.level.getBlockState(pos);
                if (!state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Vec3 turn(Vec3 flat, double degrees) {
        double a = Math.toRadians(-degrees);
        return new Vec3(flat.x * Math.cos(a) - flat.z * Math.sin(a), 0, flat.x * Math.sin(a) + flat.z * Math.cos(a));
    }

    private static Vec3 clearOfBlocks(Vec3 cam, Vec3 target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return target;
        }
        BlockHitResult hit = mc.level.clip(new ClipContext(cam, target, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return target;
        }
        Vec3 direction = target.subtract(cam).normalize();
        double distance = Math.max(1.5, hit.getLocation().distanceTo(cam) - 0.8);
        return cam.add(direction.scale(distance));
    }
}
