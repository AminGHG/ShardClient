package net.am7n.shardclient.shared.practice.ui;

import net.am7n.shardclient.shared.anim.Maths;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

public final class GlassTick extends AbstractSoundInstance implements FabricSoundInstance {
    public static final Identifier EVENT = Identifier.fromNamespaceAndPath("shardclient-shared", "ui.hover");

    private static final int RATE = 44_100;
    private static final float LENGTH_S = 0.16f;

    private static final float[][] PARTIALS = {
            {1.00f, 1.00f, 0.022f},
            {0.68f, 0.55f, 0.045f},
            {1.53f, 0.50f, 0.030f},
            {5.94f, 0.55f, 0.014f},

            {1.00f, 0.30f, 0.080f},
    };

    private final float base;

    public GlassTick(float volume) {
        super(EVENT, SoundSource.UI, RandomSource.create());
        this.volume = volume;
        this.pitch = 1f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.base = 850f * (0.95f + (float) Math.random() * 0.17f);
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeat) {
        return CompletableFuture.completedFuture(new Pcm(synthesize(base)));
    }

    static ByteBuffer synthesize(float base) {
        int samples = Math.round(RATE * LENGTH_S);
        ByteBuffer out = ByteBuffer.allocateDirect(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        double total = 0;
        for (float[] p : PARTIALS) {
            total += p[1];
        }
        for (int i = 0; i < samples; i++) {
            double t = i / (double) RATE;
            double s = 0;
            for (float[] p : PARTIALS) {
                s += p[1] * Math.exp(-t / p[2]) * Math.sin(2 * Math.PI * base * p[0] * t);
            }

            double attack = Math.min(1, t / 0.0015);
            double release = Math.min(1, (LENGTH_S - t) / 0.01);
            double value = s / total * attack * release * 0.8;
            out.putShort((short) Math.round(Maths.clamp(value, -1, 1) * 32767));
        }
        return out.flip();
    }

    private static final class Pcm implements AudioStream {
        private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
        private ByteBuffer data;

        Pcm(ByteBuffer data) {
            this.data = data;
        }

        @Override
        public AudioFormat getFormat() {
            return FORMAT;
        }

        @Override
        public ByteBuffer read(int size) {
            ByteBuffer next = data == null ? ByteBuffer.allocateDirect(0) : data;
            data = null;
            return next;
        }

        @Override
        public void close() {
            data = null;
        }
    }
}
