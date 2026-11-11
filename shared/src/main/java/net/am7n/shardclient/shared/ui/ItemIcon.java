package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.world.item.ItemStack;

/** A Minecraft item drawn as an icon, optionally sitting in a well drawn by the active look. */
public final class ItemIcon extends Element {

    private static final int ITEM = 16;

    private ItemStack stack;
    private boolean slot;

    public ItemIcon(ItemStack stack) {
        this.stack = stack;
    }

    public ItemIcon stack(ItemStack stack) {
        this.stack = stack;
        return this;
    }

    /** Draws a recessed well behind the item. */
    public ItemIcon inSlot() {
        this.slot = true;
        return this;
    }

    @Override
    public Size measure() {
        int side = slot ? Theme.look().wellSize() : ITEM;
        return new Size(side, side);
    }

    @Override
    public void render(Canvas canvas) {
        int side = slot ? Theme.look().wellSize() : ITEM;
        int left = x + (width - side) / 2;
        int top = y + (height - side) / 2;
        if (slot) {
            Theme.look().well(canvas, left, top, side);
            canvas.item(stack, left + (side - ITEM) / 2, top + (side - ITEM) / 2);
        } else {
            canvas.item(stack, left, top);
        }
    }
}
