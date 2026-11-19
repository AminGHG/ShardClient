package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.shared.render.Asset;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public final class World {
    public record View(Vec3 position, Vec3 forward, Quaternionf rotation) {
    }

    private static final RenderType CHAMS_FILL = RenderType.create("shardclient_totem_pop_chams_fill",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));
    private static final Map<Integer, RenderType> WIRES = new HashMap<>();
    private static final RenderType FLAT = RenderType.create("shardclient_flat", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .createCompositeState(false));

    private final MultiBufferSource buffers;
    private final PoseStack pose;
    private final Camera camera;
    private final float partialTick;

    World(MultiBufferSource buffers, PoseStack pose, Camera camera, float partialTick) {
        this.buffers = buffers;
        this.pose = pose;
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public static void init() {
    }

    public static void onRender(Consumer<World> handler) {
        Backend.onRender(handler);
    }

    public View view() {
        if (camera == null) {
            return null;
        }
        return new View(camera.getPosition(), new Vec3(camera.getLookVector()), new Quaternionf(camera.rotation()));
    }

    public PoseStack pose() {
        return pose;
    }

    public float partialTick() {
        return partialTick;
    }

    public void flat(PoseStack pose, Consumer<Verts> painter) {
        draw(pose, FLAT, painter);
    }

    public void glow(PoseStack pose, Asset texture, boolean seeThrough, Consumer<Verts> painter) {
        draw(pose, Glow.world(texture, seeThrough), painter);
    }

    public void chamsFill(PoseStack pose, Consumer<Verts> painter) {
        draw(pose, CHAMS_FILL, painter);
    }

    public void chamsWire(PoseStack pose, float width, Consumer<Verts> painter) {
        draw(pose, WIRES.computeIfAbsent(Math.round(width * 10), World::wire), painter);
    }

    public void text(PoseStack pose, float x, float y, Component text, boolean shadow, boolean seeThrough, int light,
                     int argb, int background, int outline) {
        Font font = Minecraft.getInstance().font;
        if (outline != 0) {
            font.drawInBatch8xOutline(text.getVisualOrderText(), x, y, argb, outline, pose.last().pose(), buffers, light);
        } else {
            font.drawInBatch(text.getVisualOrderText(), x, y, argb, shadow, pose.last().pose(), buffers,
                    seeThrough, background, light);
        }
    }

    private static RenderType wire(int tenths) {
        return RenderType.create("shardclient_totem_pop_chams_wire", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(tenths / 10.0)))
                        .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }

    private void draw(PoseStack pose, RenderType type, Consumer<Verts> painter) {
        painter.accept(new Verts(pose.last(), buffers.getBuffer(type)));
    }
}
