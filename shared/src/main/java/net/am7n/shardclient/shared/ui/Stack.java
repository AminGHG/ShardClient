package net.am7n.shardclient.shared.ui;

/**
 * Lays its children out one after another, as a row or a column, with a gap between them. Spare space along the
 * direction goes to the children that asked to {@link Element#grow(float) grow}.
 */
public final class Stack extends Container {

    public enum Align {
        START,
        CENTER,
        END,
        /** Every child is made as wide (column) or as tall (row) as the stack. */
        STRETCH
    }

    private final boolean horizontal;
    private int gap;
    private Align align = Align.STRETCH;

    private Stack(boolean horizontal, int gap) {
        this.horizontal = horizontal;
        this.gap = gap;
    }

    public static Stack row(int gap) {
        return new Stack(true, gap);
    }

    public static Stack column(int gap) {
        return new Stack(false, gap);
    }

    /** How children sit across the direction of the stack. */
    public Stack align(Align align) {
        this.align = align;
        return this;
    }

    @Override
    public Stack add(Element child) {
        super.add(child);
        return this;
    }

    @Override
    public Size measure() {
        int along = 0;
        int across = 0;
        int shown = 0;
        for (Element child : children) {
            if (!child.isVisible()) {
                continue;
            }
            Size size = child.measure();
            along += horizontal ? size.width() : size.height();
            across = Math.max(across, horizontal ? size.height() : size.width());
            shown++;
        }
        along += gap * Math.max(0, shown - 1);
        return horizontal ? new Size(along, across) : new Size(across, along);
    }

    @Override
    protected void layout() {
        int available = horizontal ? width : height;
        int crossSpace = horizontal ? height : width;
        int used = 0;
        float growTotal = 0;
        int shown = 0;
        Size[] sizes = new Size[children.size()];
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            sizes[i] = child.measure();
            used += horizontal ? sizes[i].width() : sizes[i].height();
            growTotal += child.grow();
            shown++;
        }
        used += gap * Math.max(0, shown - 1);
        int spare = Math.max(0, available - used);

        int cursor = horizontal ? x : y;
        float carried = 0;
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            if (sizes[i] == null) {
                continue;
            }
            int along = horizontal ? sizes[i].width() : sizes[i].height();
            if (growTotal > 0 && child.grow() > 0) {
                // Carry the fractions so the pieces add up to exactly the spare space.
                float share = spare * child.grow() / growTotal + carried;
                int whole = (int) share;
                carried = share - whole;
                along += whole;
            }
            int across = horizontal ? sizes[i].height() : sizes[i].width();
            int crossSize = align == Align.STRETCH ? crossSpace : Math.min(across, crossSpace);
            int crossStart = (horizontal ? y : x) + switch (align) {
                case START, STRETCH -> 0;
                case CENTER -> (crossSpace - crossSize) / 2;
                case END -> crossSpace - crossSize;
            };
            if (horizontal) {
                child.place(cursor, crossStart, along, crossSize);
            } else {
                child.place(crossStart, cursor, crossSize, along);
            }
            cursor += along + gap;
        }
    }
}
