package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.modules.Keys;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;

public final class Freelook extends Module {
    private boolean active;
    private float yaw;
    private float pitch;
    private CameraType before;

    public Freelook() {
        super("freelook", "Freelook", "Hold the freelook key (Left Alt) to look around without turning.",
                Category.VISUAL);
        setEnabled(true);
    }

    public boolean active() {
        return active;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    @Override
    public void tick() {
        boolean held = Keys.freelook != null && Keys.freelook.isDown() && Game.screen() == null && mc.player != null;
        if (held && !active) {
            active = true;
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            before = mc.options.getCameraType();
            if (before.isFirstPerson()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (!held && active) {
            stop();
        }
    }

    @Override
    protected void onDisable() {
        if (active) {
            stop();
        }
    }

    private void stop() {
        active = false;
        if (before != null) {
            mc.options.setCameraType(before);
            before = null;
        }
    }

    public void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }
}
