package com.charlesdrews.soundpad;

import android.support.annotation.FloatRange;

/**
 * Created by charlie on 9/28/17.
 */

public class PadInput {
    @FloatRange(from = 0, to = 1)
    private final float mRelativePitch;

    @FloatRange(from = 0, to = 1)
    private final float mRelativeDistortion;

    public PadInput(float relativePitch, float relativeDistortion) {
        mRelativePitch = relativePitch;
        mRelativeDistortion = relativeDistortion;
    }

    public float getRelativePitch() {
        return mRelativePitch;
    }

    public float getRelativeDistortion() {
        return mRelativeDistortion;
    }
}
