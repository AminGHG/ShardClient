package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.function.Consumer;

public final class World {
    public record View(Vec3 position, Vec3 forward, Quaternionf rotation) {
    }

    private static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Assets.id(Asset.of("shardclient", "pipeline/totem_pop_chams_fill")))
                    .withCull(true)
                    .build());
    private static final RenderPipeline WIRE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Assets.id(Asset.of("shardclient", "pipeline/totem_pop_chams_wire")))
                    .withCull(false)
                    .build());
    private static final RenderType CHAMS_FILL = RenderType.create("shardclient_totem_pop_chams_fill",
            RenderSetup.builder(FILL_PIPELINE).sortOnUpload().createRenderSetup());
    private static final RenderType CHAMS_WIRE = RenderType.create("shardclient_totem_pop_chams_wire",
            RenderSetup.builder(WIRE_PIPELINE).createRenderSetup());

    private final SubmitNodeCollector collector;
    private final PoseStack pose;
    private final GameRenderer renderer;

    World(SubmitNodeCollector collector, PoseStack pose, GameRenderer renderer) {
        this.collector = collector;
        this.pose = pose;
        this.renderer = renderer;
    }

    public static void init() {
    }

    public static void onRender(Consumer<World> handler) {
        Backend.onRender(handler);
    }

    public View view() {
        Camera camera = Backend.camera(renderer);
        if (camera == null) {
            return null;
        }
        return new View(camera.position(), new Vec3(camera.forwardVector()), new Quaternionf(camera.rotation()));
    }

    public PoseStack pose() {
        return pose;
    }

    public float partialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public void flat(PoseStack pose, Consumer<Verts> painter) {
        draw(pose, RenderTypes.textBackground(), painter);
    }

    public void glow(PoseStack pose, Asset texture, boolean seeThrough, Consumer<Verts> painter) {
        draw(pose, Glow.world(texture, seeThrough), painter);
    }

    public void chamsFill(PoseStack pose, Consumer<Verts> painter) {
        draw(pose, CHAMS_FILL, painter);
    }

    public void chamsWire(PoseStack pose, float width, Consumer<Verts> painter) {
        draw(pose, CHAMS_WIRE, painter);
    }

    public void text(PoseStack pose, float x, float y, Component text, boolean shadow, boolean seeThrough, int light,
                     int argb, int background, int outline) {
        collector.submitText(pose, x, y, text.getVisualOrderText(), shadow,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, light, argb, background, outline);
    }

    private void draw(PoseStack pose, RenderType type, Consumer<Verts> painter) {
        collector.submitCustomGeometry(pose, type, (p, consumer) -> painter.accept(new Verts(p, consumer)));
    }
}
