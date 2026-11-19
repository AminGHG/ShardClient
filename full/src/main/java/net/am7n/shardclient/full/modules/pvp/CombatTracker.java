package net.am7n.shardclient.full.modules.pvp;

import net.am7n.shardclient.shared.anim.Maths;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatTracker {
    private static final long COMBO_TIMEOUT_MS = 2500;
    private static final long TARGET_TIMEOUT_MS = 8000;

    private static double lastReach = -1;
    private static long lastReachAt;
    private static int combo;
    private static long lastHitAt;
    private static int lastOwnHurtTime;
    private static LivingEntity target;
    private static long targetAt;
    private static final Map<UUID, Integer> TOTEM_POPS = new ConcurrentHashMap<>();

    private CombatTracker() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && player == Minecraft.getInstance().player) {
                attacked(player.getEyePosition(), entity);
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(CombatTracker::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static void reset() {
        lastReach = -1;
        combo = 0;
        target = null;
        TOTEM_POPS.clear();
    }

    private static void attacked(Vec3 eye, Entity entity) {
        AABB box = entity.getBoundingBox();
        double x = Maths.clamp(eye.x, box.minX, box.maxX);
        double y = Maths.clamp(eye.y, box.minY, box.maxY);
        double z = Maths.clamp(eye.z, box.minZ, box.maxZ);
        lastReach = eye.distanceTo(new Vec3(x, y, z));
        long now = System.currentTimeMillis();
        lastReachAt = now;
        if (entity instanceof LivingEntity living) {
            if (living.hurtTime == 0) {
                combo = now - lastHitAt > COMBO_TIMEOUT_MS ? 1 : combo + 1;
                lastHitAt = now;
            }
            target = living;
            targetAt = now;
        }
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        int hurt = mc.player.hurtTime;
        if (hurt > lastOwnHurtTime) {
            combo = 0;
        }
        lastOwnHurtTime = hurt;
        if (System.currentTimeMillis() - lastHitAt > COMBO_TIMEOUT_MS) {
            combo = 0;
        }
        if (target != null && (target.isRemoved() || !target.isAlive()
                || System.currentTimeMillis() - targetAt > TARGET_TIMEOUT_MS)) {
            target = null;
        }
    }

    public static void totemPopped(Entity entity) {
        TOTEM_POPS.merge(entity.getUUID(), 1, Integer::sum);
    }

    public static int totemPops(UUID player) {
        return TOTEM_POPS.getOrDefault(player, 0);
    }

    public static double reach() {
        return System.currentTimeMillis() - lastReachAt < 5000 ? lastReach : -1;
    }

    public static int combo() {
        return combo;
    }

    public static LivingEntity target() {
        return target;
    }
}
