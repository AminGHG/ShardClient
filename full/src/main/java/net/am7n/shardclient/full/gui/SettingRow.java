package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;

/** One line of a settings list: the name on the left, its control on the right. */
final class SettingRow extends Container {

    static final int HEIGHT = 20;

    private final String label;
    private final Element control;

    SettingRow(String label, Element control) {
        this.label = label;
        this.control = control;
        add(control);
    }

    Element control() {
        return control;
    }

    @Override
    public Size measure() {
        return new Size(160, HEIGHT);
    }

    @Override
    protected void layout() {
        Size size = control.measure();
        control.place(x + width - size.width(), y + (height - size.height()) / 2, size.width(), size.height());
    }

    @Override
    public void render(Canvas canvas) {
        Look look = Theme.look();
        canvas.text(Theme.text(Component.literal(label), false), x, y + (height - 8) / 2, look.colors().textMuted(),
                look.textShadow());
        super.render(canvas);
    }
}
