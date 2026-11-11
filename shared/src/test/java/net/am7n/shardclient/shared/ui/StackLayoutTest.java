package net.am7n.shardclient.shared.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackLayoutTest {

    private static void assertBounds(Element element, int x, int y, int width, int height) {
        assertEquals(x, element.x(), "x");
        assertEquals(y, element.y(), "y");
        assertEquals(width, element.width(), "width");
        assertEquals(height, element.height(), "height");
    }

    @Test
    void aColumnMeasuresAsTheSumOfItsChildrenPlusGaps() {
        Stack column = Stack.column(4).add(new Spacer(50, 10)).add(new Spacer(80, 20)).add(new Spacer(30, 5));
        assertEquals(new Size(80, 10 + 4 + 20 + 4 + 5), column.measure());
    }

    @Test
    void hiddenChildrenTakeNoSpaceAndNoGap() {
        Element hidden = new Spacer(500, 500).visible(false);
        Stack row = Stack.row(4).add(new Spacer(10, 10)).add(hidden).add(new Spacer(10, 10));
        assertEquals(new Size(24, 10), row.measure());
    }

    @Test
    void aRowPlacesChildrenLeftToRightAndStretchesThemByDefault() {
        Element a = new Spacer(10, 6);
        Element b = new Spacer(20, 8);
        Stack row = Stack.row(4).add(a).add(b);
        row.place(100, 50, 200, 30);

        assertBounds(a, 100, 50, 10, 30);
        assertBounds(b, 114, 50, 20, 30);
    }

    @Test
    void spareSpaceGoesToGrowingChildrenByWeightAndAddsUpExactly() {
        Element fixed = new Spacer(10, 10);
        Element one = new Spacer(0, 10).grow(1);
        Element two = new Spacer(0, 10).grow(2);
        Stack row = Stack.row(0).add(fixed).add(one).add(two);
        row.place(0, 0, 110, 10);

        assertEquals(10, fixed.width());
        assertEquals(100, one.width() + two.width(), "all spare space is handed out");
        assertEquals(33, one.width());
        assertEquals(67, two.width());
        assertEquals(110, two.x() + two.width(), "the last child ends at the row's edge");
    }

    @Test
    void centreAlignmentKeepsEachChildsOwnCrossSize() {
        Element small = new Spacer(10, 10);
        Stack row = Stack.row(0).align(Stack.Align.CENTER).add(small);
        row.place(0, 0, 100, 30);
        assertBounds(small, 0, 10, 10, 10);

        Stack end = Stack.column(0).align(Stack.Align.END).add(small);
        end.place(0, 0, 100, 30);
        assertBounds(small, 90, 0, 10, 10);
    }

    @Test
    void aPanelPadsItsContent() {
        Element content = new Spacer(40, 20);
        Panel panel = new Panel(content).padding(8).minWidth(100);
        assertEquals(new Size(100, 36), panel.measure());
        panel.place(10, 10, 100, 36);
        assertBounds(content, 18, 18, 84, 20);
    }
}
