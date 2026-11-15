package net.am7n.shardclient.full.modules.lyrics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.media.MediaWatcher;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
        LevelRenderEvents.COLLECT_SUBMITS.register(this::render);
    }

    private void render(LevelRenderContext context) {
        if (!module.isEnabled() || !module.inWorld()) {
            spots.clear();
            return;
        }
        try {
            draw(context);
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.error("World lyrics failed and were switched to the screen", ex);
            module.place.set("Screen");
        }
    }

    private void draw(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = context.gameRenderer().mainCamera();
        if (mc.player == null || camera == null) {
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
        Vec3 cam = camera.position();
        PoseStack pose = context.poseStack();
        Font font = mc.font;
        Font.DisplayMode mode = module.throughWalls.on() ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        int currentId = frames.stream().filter(f -> f.role() == LyricFrame.Role.CURRENT)
                .mapToInt(LyricFrame::lineId).findFirst().orElse(-1);
        for (LyricFrame frame : frames) {
            int spotId = frame.role() == LyricFrame.Role.NEXT ? currentId : frame.lineId();
            int widthPx = font.width(frame.text());
            Spot spot = module.place.is("Follow view") ? place(camera, spotId, widthPx)
                    : spots.computeIfAbsent(spotId, id -> place(camera, id, widthPx));
            if (spot == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(spot.position().x - cam.x, spot.position().y - cam.y, spot.position().z - cam.z);
            pose.mulPose(module.facePlayer.on() || module.place.is("Follow view") ? camera.rotation() : spot.facing(cam));
            float scale = spot.scale() * frame.scale() * (frame.role() == LyricFrame.Role.NEXT ? 0.75f : 1f);
            pose.scale(scale, -scale, scale);

            FormattedCharSequence text = frame.text().getVisualOrderText();
            float x = -font.width(text) / 2f;
            float y = frame.offsetY() / (frame.role() == LyricFrame.Role.NEXT ? 0.75f : 1f) - 4;
            int alpha = Math.round(Math.clamp(frame.alpha(), 0f, 1f) * 255);

            boolean solid = module.letters.is("3D") && module.pixelFont(frame.lineId());
            if (alpha > 3 && solid) {
                StringBuilder plain = new StringBuilder();
                java.util.List<Integer> colors = new java.util.ArrayList<>();
                frame.text().visit((style, part) -> {
                    int rgb = style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
                    part.codePoints().forEach(cp -> {
                        plain.appendCodePoint(cp);
                        colors.add(alpha << 24 | rgb);
                    });
                    return java.util.Optional.empty();
                }, net.minecraft.network.chat.Style.EMPTY);
                String line = plain.toString();
                int[] perChar = colors.stream().mapToInt(Integer::intValue).toArray();
                boolean bold = module.bold.on();
                float voxelX = -VoxelFont.width(line, bold) / 2f;
                float depth = module.depth.floatValue();

                context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.textBackground(),
                        (p, consumer) -> VoxelFont.draw(p, consumer, line, perChar.length == 0 ? new int[]{alpha << 24
                                | 0xFFFFFF} : perChar, voxelX, y, depth, bold, FULL_BRIGHT));
            }
            if (alpha > 3 && !solid) {
                float glow = module.glowStrength();
                if (glow > 0 && frame.role() != LyricFrame.Role.NEXT) {
                    int g = module.glowColor(frame.color()) & 0xFFFFFF;
                    int glowAlpha = Math.round(alpha * glow);
                    FormattedCharSequence halo = LyricsModule.recolor(frame.text(), g).getVisualOrderText();

                    for (int ring = 1; ring <= 3; ring++) {
                        float radius = ring * 0.55f;
                        int ringAlpha = Math.max(1, Math.round(glowAlpha * (0.55f - ring * 0.13f)));
                        for (int step = 0; step < 12; step++) {
                            double angle = step * Math.PI / 6 + ring * 0.26;
                            context.submitNodeCollector().submitText(pose, x + (float) Math.cos(angle) * radius,
                                    y + (float) Math.sin(angle) * radius, halo, false, mode, FULL_BRIGHT,
                                    ringAlpha << 24 | g, 0, 0);
                        }
                    }
                }
                int outline = module.outline.on() ? alpha << 24 : 0;
                context.submitNodeCollector().submitText(pose, x, y, text, module.shadow.on() && !module.outline.on(),
                        mode, FULL_BRIGHT, alpha << 24 | 0xFFFFFF, 0, outline);
            }
            pose.popPose();
        }

        spots.keySet().removeIf(id -> Math.abs(id - currentId) > 2);
    }

    private Spot place(Camera camera, int id, int widthPx) {
        Vec3 cam = camera.position();
        Vec3 look = new Vec3(camera.forwardVector());
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
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(sample);
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
