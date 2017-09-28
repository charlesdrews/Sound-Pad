package com.charlesdrews.soundpad;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView;
    private int mWidth, mHeight;
    //    private ToneGenerator mToneGenerator;
    private SoundGenerator mSoundGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.textBox);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mWidth = displayMetrics.widthPixels;
        mHeight = displayMetrics.heightPixels;

        mSoundGenerator = new SoundGenerator();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onDestroy() {
        mSoundGenerator.stop();
        mSoundGenerator.releaseResources();

        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / mWidth;
        float y = 1 - (event.getY() / mHeight);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                displayCoordinates(x, y);
                try {
                    mSoundGenerator.start(y, x);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error starting sound generation", Toast.LENGTH_SHORT).show();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                displayCoordinates(x, y);
                try {
                    mSoundGenerator.update(y, x);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error updating pitch", Toast.LENGTH_SHORT).show();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mSoundGenerator.stop();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void displayCoordinates(float x, float y) {
        mTextView.setText(String.format(Locale.getDefault(), "x: %.0f%%\ny: %.0f%%",
                x * 100, y * 100));
    }
}
