package com.charlesdrews.soundpad;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.support.annotation.FloatRange;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Uses wavetable synthesis to generate sound with various pitches and waveforms
 * Created by charlie on 9/28/17.
 */

class SoundGenerator {
    private static final String TAG = "SoundGenerator";

    private static final int SAMPLE_RATE = 44_100;
    private static final int SAMPLE_COUNT = 2048;

    private static final double MIN_PITCH_IN_HZ = Math.max(400d,
            (double) SAMPLE_RATE / (double) SAMPLE_COUNT);
    private static final double MAX_PITCH_IN_HZ = Math.min(4000d, (double) SAMPLE_RATE / 2d);
    static final int NUM_OCTAVES =
            (int) (Math.log(MAX_PITCH_IN_HZ / MIN_PITCH_IN_HZ) / Math.log(2));

    private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_SAMPLE_VALUE = Short.MAX_VALUE;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING_FORMAT);

    private static final int SINE = 0, TRIANGLE = 1, SQUARE = 2, SAWTOOTH = 3;
    static final int NUM_WAVE_FORMS = 4;
    private static final double[][] WAVES = new double[NUM_WAVE_FORMS][SAMPLE_COUNT];
    private static final double SQUARE_WAVE_VOL_ADJ = 0.4;
    private static final double SAW_WAVE_VOL_ADJ = 0.7;

    private final AudioTrack mAudioTrack;
    private final BlockingQueue<Input> mInputQueue;

    private volatile boolean mPlaying = false;

    SoundGenerator() {

        mInputQueue = new LinkedBlockingQueue<>();

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

        generateTables();
    }


    //---------------------------------------------------------------------------------------
    //---------- Public Methods
    //---------------------------------------------------------------------------------------
    void start(double initialRelativePitch, double initialRelativeHarmonics) {
        // put initial input in input queue & initialize track
        update(initialRelativePitch, initialRelativeHarmonics);
        mAudioTrack.play();
        mPlaying = true;

        // play audio from background thread
        new Thread(() -> {
            short[] sample = new short[SAMPLE_COUNT];
            double currentPitchInHz = MIN_PITCH_IN_HZ;
            int numSamples = SAMPLE_COUNT;

            // Main sound generation loop - check for new inputs on each iteration
            while (mPlaying) {
                Input padInput;
                if (mInputQueue.size() > 0 && (padInput = mInputQueue.poll()) != null) {
                    currentPitchInHz = MIN_PITCH_IN_HZ * Math.pow(2d, NUM_OCTAVES * padInput.getRelativePitch());
                    numSamples = generateSample(sample, currentPitchInHz, (float) padInput.getRelativeDistortion(), 1);
                }
                mAudioTrack.write(sample, 0, numSamples);
            }


            // Fade out and stop (but not if AudioTrack is uninitialized)
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                float gain = 1f;
                while (gain > 0.01f) {
                    gain *= 0.9f;
                    numSamples = generateSample(sample, currentPitchInHz, 0, gain);
                    mAudioTrack.write(sample, 0, numSamples);
                }
                mAudioTrack.stop();
            }

            mInputQueue.clear();
        }).start();
    }


    void update(@FloatRange(from = 0, to = 1) double relativePitch,
                @FloatRange(from = 0, to = 1) double relativeDistortion) {
        try {
            mInputQueue.put(new Input(relativePitch, relativeDistortion));
        } catch (InterruptedException e) {
            Log.d(TAG, "update: Error writing to input queue");
            e.printStackTrace();
        }
    }

    void stop() {
        mPlaying = false;
    }


    //---------------------------------------------------------------------------------------
    //---------- Helper Methods
    //---------------------------------------------------------------------------------------
    private int generateSample(short[] sampleHolder, double pitchInHz,
                               @FloatRange(from = 0, to = 1) float distortion,
                               @FloatRange(from = 0, to = 1) float gain) {

        distortion = Math.min(0.99f, Math.max(0.01f, distortion));
        float avgWaveType = distortion * (NUM_WAVE_FORMS - 1);
        int waveTypeOne = (int) Math.floor(avgWaveType);
        int waveTypeTwo = (int) Math.ceil(avgWaveType);
        float weightOne = waveTypeTwo - avgWaveType;
        float weightTwo = 1 - weightOne;

        int i = 0, step = 0;
        int stepSize = (int) Math.round(pitchInHz * SAMPLE_COUNT / SAMPLE_RATE);
        while (step < SAMPLE_COUNT) {
            sampleHolder[i] = (short) (((WAVES[waveTypeOne][step] * weightOne) +
                                (WAVES[waveTypeTwo][step] * weightTwo)) * gain);
            i++;
            step += stepSize;
        }
        return i;
    }

    void releaseResources() {
        mAudioTrack.release();
    }

    private void generateTables() {
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double radians = 2d * Math.PI * i / SAMPLE_COUNT;

            WAVES[SINE][i] = Math.sin(radians) * MAX_SAMPLE_VALUE;

            WAVES[TRIANGLE][i] = (radians < Math.PI ?
                    -1d + (2d * radians / Math.PI) :
                    3d - (2d * radians / Math.PI)) * MAX_SAMPLE_VALUE;

            WAVES[SQUARE][i] = (radians < Math.PI ? 1d : -1d) * SQUARE_WAVE_VOL_ADJ * MAX_SAMPLE_VALUE;

            WAVES[SAWTOOTH][i] = (-1d + radians / Math.PI) * SAW_WAVE_VOL_ADJ * MAX_SAMPLE_VALUE;
        }
    }


    //---------------------------------------------------------------------------------------
    //---------- Nested Classes
    //---------------------------------------------------------------------------------------

    /**
     * Holder for the 2d inputs coming from the XYInputPad that need to be queued
     */
    protected class Input {
        @FloatRange(from = 0, to = 1)
        private final double mRelativePitch;

        @FloatRange(from = 0, to = 1)
        private final double mRelativeDistortion;

        Input(double relativePitch, double relativeDistortion) {
            mRelativePitch = relativePitch;
            mRelativeDistortion = relativeDistortion;
        }

        double getRelativePitch() {
            return mRelativePitch;
        }

        double getRelativeDistortion() {
            return mRelativeDistortion;
        }
    }
}
