package com.charlesdrews.soundpad;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity implements XYInputPad.XYInputPadListener {
    private static final String TAG = "MainActivity";

    private SoundGenerator mSoundGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSoundGenerator = new SoundGenerator();

        XYInputPad XYInputPad = findViewById(R.id.xyInputPad);
        XYInputPad.setListener(this);
        XYInputPad.setDivisions(SoundGenerator.NUM_WAVE_FORMS - 1, SoundGenerator.NUM_OCTAVES);

        // Apply two gradients to the background of the input pad
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {
                getDrawable(R.drawable.horiz_gradient), getDrawable(R.drawable.vert_gradient)});
        XYInputPad.setBackground(layerDrawable);

        // Make physical volume buttons control media volume (as opposed to alarm volume by default)
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onDestroy() {
        mSoundGenerator.stop();
        mSoundGenerator.releaseResources();

        super.onDestroy();
    }

    @Override
    public void down(double relativeX, double relativeY) {
        mSoundGenerator.start(1 - relativeY, relativeX);
    }

    @Override
    public void move(double relativeX, double relativeY) {
        mSoundGenerator.update(1 - relativeY, relativeX);
    }

    @Override
    public void up() {
        mSoundGenerator.stop();
    }
}
