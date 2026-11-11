package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.protocol.packet.Notification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The client's own interface sounds, built from Minecraft's sounds so nothing extra is downloaded. They sit on
 * top of the sounds the server already plays for everyone, so they are short and pitched higher than those to
 * add sparkle rather than noise. Only this player hears them.
 */
public final class UiSounds {

    /** Semitone steps of the Match Found arpeggio (a major chord rising to the octave), from F#. */
    private static final int[] MATCH_ARPEGGIO = {0, 4, 7, 12};
    private static final int ARPEGGIO_STEP_TICKS = 2;

    /** Off switch for the settings to come. */
    private static volatile boolean enabled = true;

    private UiSounds() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void matchFound() {
        for (int i = 0; i < MATCH_ARPEGGIO.length; i++) {
            play(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), pitch(MATCH_ARPEGGIO[i]), 0.55f,
                    i * ARPEGGIO_STEP_TICKS);
        }
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.7f, MATCH_ARPEGGIO.length * ARPEGGIO_STEP_TICKS);
    }

    public static void roundWon() {
        notes(new int[]{7, 12}, 0.5f);
    }

    public static void roundLost() {
        notes(new int[]{0, -5}, 0.4f);
    }

    public static void matchWon() {
        notes(new int[]{0, 4, 7, 12, 16}, 0.55f);
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 0.7f, 5 * ARPEGGIO_STEP_TICKS);
    }

    public static void matchLost() {
        notes(new int[]{0, -3, -7}, 0.45f);
    }

    private static void notes(int[] semitones, float volume) {
        for (int i = 0; i < semitones.length; i++) {
            play(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), pitch(semitones[i]), volume, i * ARPEGGIO_STEP_TICKS);
        }
    }

    public static void queueJoined() {
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.8f, 0);
    }

    public static void queueLeft() {
        play(SoundEvents.UI_TOAST_OUT, 1.1f, 0.6f, 0);
    }

    public static void notification(Notification.Level level) {
        play(SoundEvents.UI_TOAST_IN, 1.1f, 0.6f, 0);
        if (level == Notification.Level.WARNING || level == Notification.Level.ERROR) {
            play(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), pitch(-6), 0.45f, 1);
        }
    }

    /** Note-block pitch for {@code semitones} above F#. */
    private static float pitch(int semitones) {
        return (float) Math.pow(2, semitones / 12.0);
    }

    private static void play(SoundEvent sound, float pitch, float volume, int delayTicks) {
        if (!enabled) {
            return;
        }
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        SimpleSoundInstance instance = SimpleSoundInstance.forUI(sound, pitch, volume);
        if (delayTicks <= 0) {
            sounds.play(instance);
        } else {
            sounds.playDelayed(instance, delayTicks);
        }
    }
}
