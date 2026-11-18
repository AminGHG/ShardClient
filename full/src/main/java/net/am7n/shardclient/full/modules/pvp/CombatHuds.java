package net.am7n.shardclient.full.modules.pvp;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CombatHuds {
    private CombatHuds() {
    }

    private abstract static class Line extends HudModule {
        Line(String id, String name, String description, float x, float y) {
            super(id, name, description, x, y);
        }

        protected abstract Component text();

        @Override
        public Size size(Canvas canvas) {
            return textSize(canvas, text());
        }

        @Override
        public void draw(Canvas canvas, int x, int y) {
            drawText(canvas, x, y, text());
        }
    }

    public static final class Reach extends Line {
        public Reach() {
            super("reach", "Reach", "How far away your last hit landed.", 0.5f, 0.62f);
        }

        @Override
        public boolean visible() {
            return CombatTracker.reach() >= 0;
        }

        @Override
        protected Component text() {
            return labelled("Reach", String.format(Locale.ROOT, "%.2f", Math.max(0, CombatTracker.reach())));
        }
    }

    public static final class Combo extends Line {
        public Combo() {
            super("combo", "Combo", "Hits in a row without being hit back.", 0.5f, 0.70f);
        }

        @Override
        public boolean visible() {
            return CombatTracker.combo() > 0;
        }

        @Override
        protected Component text() {
            return labelled("Combo", Integer.toString(CombatTracker.combo()));
        }
    }

    public static final class TotemPops extends Line {
        public TotemPops() {
            super("totem_pops", "Totem pops", "How many totems you and your target have used.", 1.0f, 0.55f);
        }

        @Override
        public boolean visible() {
            return mc.player != null && (CombatTracker.totemPops(mc.player.getUUID()) > 0
                    || CombatTracker.target() instanceof Player);
        }

        @Override
        protected Component text() {
            String value = "You " + CombatTracker.totemPops(mc.player.getUUID());
            if (CombatTracker.target() instanceof Player target) {
                value += "  ·  " + target.getName().getString() + " " + CombatTracker.totemPops(target.getUUID());
            }
            return labelled("Totems", value);
        }
    }

    public static final class Cooldowns extends HudModule {
        private List<ItemStack> items;

        public Cooldowns() {
            super("cooldowns", "Cooldowns", "Ender pearl, wind charge, shield and chorus fruit cooldowns.",
                    0.5f, 0.82f);
        }

        private List<ItemStack> active() {
            if (items == null) {
                items = List.of(new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.WIND_CHARGE),
                        new ItemStack(Items.SHIELD), new ItemStack(Items.CHORUS_FRUIT));
            }
            List<ItemStack> out = new ArrayList<>();
            for (ItemStack stack : items) {
                if (mc.player.getCooldowns().isOnCooldown(stack)) {
                    out.add(stack);
                }
            }
            return out;
        }

        @Override
        public boolean visible() {
            return mc.player != null && !active().isEmpty();
        }

        @Override
        public Size size(Canvas canvas) {
            return new Size(PADDING * 2 + active().size() * 22 - 4, PADDING * 2 + 16 + 5);
        }

        @Override
        public void draw(Canvas canvas, int x, int y) {
            Size size = size(canvas);
            drawBackground(canvas, x, y, size.width(), size.height());
            Palette colors = Theme.colors();
            int ix = x + PADDING;
            for (ItemStack stack : active()) {
                canvas.item(stack, ix, y + PADDING);
                float left = mc.player.getCooldowns().getCooldownPercent(stack, 0f);
                canvas.fill(ix, y + PADDING + 18, 16, 2, colors.ghost());
                canvas.fill(ix, y + PADDING + 18, Math.round(16 * left), 2, colors.accentStrong());
                ix += 22;
            }
        }
    }

    public static final class Target extends HudModule {
        private final BooleanSetting players = bool("players_only", "Players only", true);

        public Target() {
            super("target", "Target info", "Health and armour of the player you are fighting.", 0.62f, 0.55f);
        }

        private LivingEntity target() {
            LivingEntity target = CombatTracker.target();
            return target != null && (!players.on() || target instanceof Player) ? target : null;
        }

        @Override
        public boolean visible() {
            return target() != null;
        }

        @Override
        public Size size(Canvas canvas) {
            return new Size(130, 36);
        }

        @Override
        public void draw(Canvas canvas, int x, int y) {
            LivingEntity target = target();
            if (target == null) {
                return;
            }
            Palette colors = Theme.colors();
            drawBackground(canvas, x, y, 130, 36);
            int textX = x + PADDING;
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(target.getUUID());
            if (info != null) {
                canvas.face(target.getUUID(), x + PADDING, y + PADDING, 28);
                textX += 32;
            }
            canvas.text(Theme.text(target.getName(), true), textX, y + PADDING + 1, colors.text(), false);
            float health = target.getHealth() + target.getAbsorptionAmount();
            float max = Math.max(1, target.getMaxHealth());
            String hp = String.format(Locale.ROOT, "%.1f ❤", health / 2f);
            canvas.text(Component.literal(hp), textX, y + PADDING + 12, 0xFFFF6B6B, net.am7n.shardclient.shared.render.Theme.mcShadow());
            canvas.textRight(Component.literal(target.getArmorValue() + " armor"), x + 130 - PADDING, y + PADDING + 12,
                    colors.textMuted(), net.am7n.shardclient.shared.render.Theme.mcShadow());
            int barWidth = 130 - PADDING - (textX - x);
            canvas.fill(textX, y + 36 - PADDING - 2, barWidth, 2, colors.ghost());
            canvas.fill(textX, y + 36 - PADDING - 2, Math.round(barWidth * Math.min(1f, health / max)), 2, 0xFFFF6B6B);
        }
    }
}
