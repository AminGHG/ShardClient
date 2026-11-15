package net.am7n.shardclient.shared.ui;

public interface Focusable {
    void setFocused(boolean focused);

    void activate();

    boolean canFocus();
}
