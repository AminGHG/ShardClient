package net.am7n.shardclient.shared.anim;

/**
 * The motion rules every ShardClient interface follows, as values to use instead of inventing new ones.
 *
 * <ol>
 *   <li>Entrances ease out ({@link #ENTER}).</li>
 *   <li>Dropdowns and popovers take 150–250 ms ({@link #POPOVER_MS}).</li>
 *   <li>List rows are staggered by 30 ms ({@link #ROW_STAGGER_MS}).</li>
 *   <li>A pressed control scales to 0.97 ({@link #PRESS_SCALE}).</li>
 *   <li>A moving knob stretches 12% along its travel ({@link #KNOB_STRETCH}).</li>
 *   <li>Details settle one after another, not all at once (use {@link Stagger}).</li>
 *   <li>Objects that travel move on arcs, not straight lines.</li>
 *   <li>Success is confirmed with a pulse ({@link #PULSE_SCALE}).</li>
 *   <li>Moves from one place to another ease in and out ({@link #MOVE}).</li>
 *   <li>Popovers start at 0.95 scale ({@link #POPOVER_START_SCALE}).</li>
 *   <li>Things enter from the control that opened them...</li>
 *   <li>...and exit back to it.</li>
 *   <li>No bounce on everyday UI: {@link Easing#OUT_BACK} is for rare celebrations only.</li>
 *   <li>Nothing tweens while the pointer holds it: follow the pointer directly, animate on release.</li>
 *   <li>Exits are 20% shorter than entrances ({@link #exitMs}).</li>
 *   <li>Old and new states overlap (crossfade) instead of one ending before the other starts.</li>
 *   <li>Reveals and colour changes use masks (clip regions), not hard swaps.</li>
 *   <li>Icons change by crossfading.</li>
 *   <li>Dragging past a limit rubber-bands ({@link #rubberBand}).</li>
 *   <li>One element shown in two places is the same element moving, not two.</li>
 * </ol>
 */
public final class Motion {

    private Motion() {
    }

    public static final Easing ENTER = Easing.OUT_CUBIC;
    public static final Easing EXIT = Easing.IN_CUBIC;
    public static final Easing MOVE = Easing.IN_OUT_CUBIC;

    public static final long POPOVER_MS = 200;
    public static final long ROW_STAGGER_MS = 30;
    public static final long PRESS_RELEASE_MS = 140;

    public static final float PRESS_SCALE = 0.97f;
    public static final float POPOVER_START_SCALE = 0.95f;
    public static final float KNOB_STRETCH = 0.12f;
    public static final float PULSE_SCALE = 1.06f;

    /** How long the exit of something that entered in {@code enterMs} takes: 20% shorter. */
    public static long exitMs(long enterMs) {
        return Math.round(enterMs * 0.8);
    }

    /** A transition that enters in {@code enterMs} and exits by the rules above. */
    public static Transition transition(long enterMs) {
        return new Transition(enterMs, ENTER, exitMs(enterMs), EXIT);
    }

    /**
     * Where something dragged {@code overshoot} pixels past a limit should be drawn: it follows, but less and
     * less, and never further than {@code max}.
     */
    public static float rubberBand(float overshoot, float max) {
        if (max <= 0) {
            return 0;
        }
        float sign = Math.signum(overshoot);
        float distance = Math.abs(overshoot);
        return sign * max * (1 - 1 / (distance / max + 1));
    }
}
