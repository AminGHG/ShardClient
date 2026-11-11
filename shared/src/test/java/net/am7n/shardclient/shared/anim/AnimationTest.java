package net.am7n.shardclient.shared.anim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {

    /** A clock the test moves by hand. */
    private static final class ManualClock implements Clock {
        private long nanos = 1_000_000_000L;

        @Override
        public long nanos() {
            return nanos;
        }

        void advanceMs(long ms) {
            nanos += ms * 1_000_000L;
        }
    }

    private static final float EPSILON = 1e-4f;

    @Test
    void everyEasingStartsAtZeroAndEndsAtOne() {
        List<Easing> all = List.of(Easing.LINEAR, Easing.IN_CUBIC, Easing.OUT_CUBIC, Easing.IN_OUT_CUBIC,
                Easing.OUT_QUART, Easing.OUT_EXPO, Easing.OUT_BACK, Easing.IN_OUT_SINE);
        for (Easing easing : all) {
            assertEquals(0f, easing.apply(0f), 2e-3f);
            assertEquals(1f, easing.apply(1f), EPSILON);
        }
    }

    @Test
    void aTweenArrivesAtItsTarget() {
        ManualClock clock = new ManualClock();
        Tween tween = new Tween(0, 200, Easing.LINEAR, clock);
        tween.set(10);
        clock.advanceMs(100);
        assertEquals(5f, tween.get(), EPSILON);
        assertFalse(tween.isSettled());
        clock.advanceMs(100);
        assertEquals(10f, tween.get(), EPSILON);
        assertTrue(tween.isSettled());
    }

    @Test
    void retargetingATweenContinuesFromWhereItIs() {
        ManualClock clock = new ManualClock();
        Tween tween = new Tween(0, 200, Easing.OUT_CUBIC, clock);
        tween.set(10);
        clock.advanceMs(60);
        float before = tween.get();
        tween.set(0);
        assertEquals(before, tween.get(), EPSILON, "no jump at the moment of reversal");
        clock.advanceMs(200);
        assertEquals(0f, tween.get(), EPSILON);
    }

    @Test
    void snappingSkipsTheAnimation() {
        ManualClock clock = new ManualClock();
        Tween tween = new Tween(0, 200, Easing.LINEAR, clock);
        tween.set(10);
        tween.snap(3);
        assertEquals(3f, tween.get(), EPSILON);
        assertTrue(tween.isSettled());
    }

    @Test
    void aTransitionWalksThroughItsStates() {
        ManualClock clock = new ManualClock();
        Transition transition = new Transition(200, Easing.LINEAR, 100, Easing.LINEAR, clock);
        assertEquals(Transition.State.HIDDEN, transition.state());
        assertFalse(transition.isVisible());

        transition.show();
        clock.advanceMs(100);
        assertEquals(Transition.State.ENTERING, transition.state());
        assertEquals(0.5f, transition.progress(), EPSILON);

        clock.advanceMs(100);
        assertEquals(Transition.State.SHOWN, transition.state());

        transition.hide();
        clock.advanceMs(50);
        assertEquals(Transition.State.EXITING, transition.state());
        assertEquals(0.5f, transition.progress(), EPSILON);

        clock.advanceMs(50);
        assertEquals(Transition.State.HIDDEN, transition.state());
    }

    @Test
    void reversingATransitionHalfWayNeitherJumpsNorTakesTheFullTime() {
        ManualClock clock = new ManualClock();
        Transition transition = new Transition(200, Easing.OUT_CUBIC, 200, Easing.IN_CUBIC, clock);
        transition.show();
        clock.advanceMs(80);
        float before = transition.progress();

        transition.hide();
        assertEquals(before, transition.progress(), EPSILON, "no jump when turning around");

        // It only has `before` of the way to go back, so it needs that share of the exit time.
        clock.advanceMs((long) (200 * before) + 1);
        assertEquals(Transition.State.HIDDEN, transition.state());
    }

    @Test
    void showingTwiceDoesNotRestart() {
        ManualClock clock = new ManualClock();
        Transition transition = new Transition(200, Easing.LINEAR, 200, Easing.LINEAR, clock);
        transition.show();
        clock.advanceMs(150);
        transition.show();
        assertEquals(0.75f, transition.progress(), EPSILON);
    }

    @Test
    void staggeredItemsStartOneAfterAnotherAndAllFinish() {
        Stagger stagger = new Stagger(4, 50, 100);
        assertEquals(250, stagger.totalMs());

        float half = 0.5f; // 125 ms in
        assertEquals(1f, stagger.item(half, 0, Easing.LINEAR), EPSILON);
        assertEquals(0.75f, stagger.item(half, 1, Easing.LINEAR), EPSILON);
        assertEquals(0.25f, stagger.item(half, 2, Easing.LINEAR), EPSILON);
        assertEquals(0f, stagger.item(half, 3, Easing.LINEAR), EPSILON);

        for (int i = 0; i < 4; i++) {
            assertEquals(0f, stagger.item(0f, i, Easing.OUT_CUBIC), EPSILON);
            assertEquals(1f, stagger.item(1f, i, Easing.OUT_CUBIC), EPSILON);
        }
    }

    @Test
    void aCycleRepeats() {
        ManualClock clock = new ManualClock();
        Cycle cycle = new Cycle(1000, clock);
        assertEquals(0f, cycle.phase(), EPSILON);
        clock.advanceMs(500);
        assertEquals(0.5f, cycle.phase(), EPSILON);
        assertEquals(1f, cycle.wave(), EPSILON);
        clock.advanceMs(750);
        assertEquals(0.25f, cycle.phase(), EPSILON);
    }

    @Test
    void coloursMixPerChannel() {
        assertEquals(0x80808080, Lerp.color(0x00000000, 0xFFFFFFFF, 0.5f));
        assertEquals(0xFF102030, Lerp.color(0xFF102030, 0x00000000, 0f));
        assertEquals(0xFFD2C0FF, Lerp.color(0xFF000000, 0xFFD2C0FF, 1f));
    }
}
