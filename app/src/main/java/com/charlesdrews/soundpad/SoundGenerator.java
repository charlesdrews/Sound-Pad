package com.charlesdrews.soundpad;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.support.annotation.FloatRange;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by charlie on 9/28/17.
 */

public class SoundGenerator {
    private static final String TAG = "SoundGenerator";

    private static final int MIN_PITCH_IN_HZ = 220;
    private static final int NUM_OCTAVES = 3;
    private static final int SAMPLES_PER_WAVELENGTH = 50;
    private static final int SAMPLE_RATE = (int) (MIN_PITCH_IN_HZ * Math.pow(2, NUM_OCTAVES) * SAMPLES_PER_WAVELENGTH); //AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING_FORMAT);

    private final AudioTrack mAudioTrack;
    private final BlockingQueue<PadInput> mPitchQueue;

    private volatile boolean mPlaying = false;

    public SoundGenerator() {

        mPitchQueue = new LinkedBlockingQueue<>();

        mAudioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(ENCODING_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(BUFFER_SIZE)
                .build();

        mAudioTrack.setVolume(1f);
    }

    /**************************************************************
     *** Public methods
     **************************************************************/
    public void start(float initialRelativePitch, float initialRelativeHarmonics) throws InterruptedException {
        mAudioTrack.play();
        mPlaying = true;

        new Thread(() -> {
            short[] sample = new short[SAMPLES_PER_WAVELENGTH];
            int currentPitchInHz = getAbsolutePitch(initialRelativePitch);
            generateSample(sample, currentPitchInHz, initialRelativeHarmonics, 1);

            while (mPlaying) {
                PadInput padInput;
                if (mPitchQueue.size() > 0 && (padInput = mPitchQueue.poll()) != null) {
                    currentPitchInHz = getAbsolutePitch(padInput.getRelativePitch());
                    generateSample(sample, currentPitchInHz, padInput.getRelativeDistortion(), 1);
                }
                mAudioTrack.write(sample, 0, sample.length);
            }

            // Fade out and stop (but not if AudioTrack is uninitialized)
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                float gain = 1f;
                while (gain > 0.01f) {
                    gain *= 0.9f;
                    generateSample(sample, currentPitchInHz, 0, gain);
                    mAudioTrack.write(sample, 0, sample.length);
                }
                mAudioTrack.stop();
            }

            mPitchQueue.clear();
        }).start();
    }


    public void update(
            @FloatRange(from = 0, to = 1) float relativePitch,
            @FloatRange(from = 0, to = 1) float relativeHarmonics
    ) throws InterruptedException {
        mPitchQueue.put(new PadInput(relativePitch, relativeHarmonics));
    }

    public void stop() {
        mPlaying = false;
    }

    /**************************************************************
     *** Helper methods
     **************************************************************/
    private int getAbsolutePitch(@FloatRange(from = 0, to = 1) float relativePitch) {
        return (int) (MIN_PITCH_IN_HZ * Math.pow(2, NUM_OCTAVES * relativePitch));
    }

    private void generateSample(short[] sampleHolder, int pitchInHz,
                                @FloatRange(from = 0, to = 1) float distortion,
                                @FloatRange(from = 0, to = 1) float gain) {
        int sampleRateInHz = pitchInHz * sampleHolder.length;
        mAudioTrack.setPlaybackRate(sampleRateInHz);
        for (int i = 0; i < sampleHolder.length; i++) {
            double frequencyAtI = (double) (pitchInHz * i) / sampleRateInHz;

            /* These are just too slow
            // generate square or sawtooth (pure sine if harmonics = 1)
            double sum = 0;
            for (int n = 1; n <= harmonics; n++) {
                // sawtooth
                sum += 1 - (Math.pow(-1, n) * Math.sin(2 * Math.PI * n * frequencyAtI) / n);

                // square
                //sum += Math.sin(2 * Math.PI * ((2 * n) - 1) * frequencyAtI) / ((2 * n) - 1);
            }
            sampleHolder[i] = (short) (sum * gain * Short.MAX_VALUE);
            */

            double sineSample = Math.sin(2 * Math.PI * frequencyAtI);
            double squareSample = Math.signum(sineSample);
            double blendedSample = ((1 - distortion) * sineSample) + (distortion * squareSample);
            sampleHolder[i] = (short) (blendedSample * gain * Short.MAX_VALUE);
        }
    }

    public void releaseResources() {
        mAudioTrack.release();
    }
}
