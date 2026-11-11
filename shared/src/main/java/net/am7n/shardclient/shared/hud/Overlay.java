package net.am7n.shardclient.shared.hud;

import net.am7n.shardclient.shared.render.Canvas;

/**
 * Something drawn over the game while playing (a notification, a queue indicator), without taking the mouse
 * or keyboard. It owns its own entrance and exit animation.
 */
public interface Overlay {

    /**
     * Draws one frame.
     *
     * @return false once the overlay has finished (its exit animation is over): it is then removed
     */
    boolean render(Canvas canvas);
}
