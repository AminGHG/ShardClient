package net.am7n.shardclient.shared.ui;

/** An element the keyboard can reach: Tab moves between them, Enter or Space activates the focused one. */
public interface Focusable {

    void setFocused(boolean focused);

    /** Enter or Space was pressed while focused. */
    void activate();

    /** False while disabled or hidden: Tab skips it. */
    boolean canFocus();
}
