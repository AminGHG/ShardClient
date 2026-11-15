package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHud extends HudModule {
    private static final List<EquipmentSlot> ARMOR =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final int ROW = 18;

    private final BooleanSetting held = bool("held", "Held item", true);
    private final ChoiceSetting value = choice("value", "Durability as", "Percent", "Percent", "Points");

    public ArmorHud() {
        super("armor", "Armor status", "Your armour and held item with their durability.", 1.0f, 0.75f);
    }

    private List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        if (held.on() && !mc.player.getMainHandItem().isEmpty()) {
            out.add(mc.player.getMainHandItem());
        }
        return out;
    }

    @Override
    public boolean visible() {
        return mc.player != null && !stacks().isEmpty();
    }

    private String durability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "";
        }
        int left = stack.getMaxDamage() - stack.getDamageValue();
        return value.is("Points") ? Integer.toString(left) : left * 100 / Math.max(1, stack.getMaxDamage()) + "%";
    }

    @Override
    public Size size(Canvas canvas) {
        int textWidth = 0;
        for (ItemStack stack : stacks()) {
            textWidth = Math.max(textWidth, canvas.textWidth(Component.literal(durability(stack))));
        }
        return new Size(PADDING * 2 + 16 + 4 + textWidth, PADDING * 2 + stacks().size() * ROW - 2);
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        Size size = size(canvas);
        drawBackground(canvas, x, y, size.width(), size.height());
        Palette colors = Theme.colors();
        int rowY = y + PADDING;
        for (ItemStack stack : stacks()) {
            canvas.item(stack, x + PADDING, rowY);
            int color = colors.text();
            if (stack.isDamageableItem()) {
                float left = 1f - stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage());
                color = left < 0.15f ? 0xFFFF5A5A : left < 0.4f ? 0xFFFFC857 : colors.text();
            }
            canvas.text(Component.literal(durability(stack)), x + PADDING + 20, rowY + 4, color, net.am7n.shardclient.shared.render.Theme.mcShadow());
            rowY += ROW;
        }
    }
}
