package com.charlesdrews.soundpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Takes in 2d input from the user
 * Created by charlie on 9/30/17.
 */

public class XYInputPad extends View {

    private static final int BORDER_COLOR = Color.BLACK;
    private static final float BORDER_WIDTH_PX = 15f;

    private static final int DIVIDER_COLOR = Color.GRAY;
    private static final float DIVIDER_WIDTH_PX = 1f;

    private int mWidth, mHeight, mColumns = 1, mRows = 1;
    private Paint mBorderPaint, mDividerPaint;
    private XYInputPadListener mListener;

    public XYInputPad(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(BORDER_COLOR);
        mBorderPaint.setStrokeWidth(BORDER_WIDTH_PX);
        mBorderPaint.setStyle(Paint.Style.STROKE);

        mDividerPaint = new Paint();
        mDividerPaint.setColor(DIVIDER_COLOR);
        mDividerPaint.setStrokeWidth(DIVIDER_WIDTH_PX);
        mDividerPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mWidth = canvas.getWidth();
        mHeight = canvas.getHeight();

        for (int i = 1; i < mColumns; i++) {
            float x = i * mWidth / mColumns;
            canvas.drawLine(x, 0, x, mHeight, mDividerPaint);
        }

        for (int i = 1; i < mRows; i++) {
            float y = i * mHeight / mRows;
            canvas.drawLine(0, y, mWidth, y, mDividerPaint);
        }

        canvas.drawRect(0, 0, mWidth, mHeight, mBorderPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener != null) {
            double relativeX = (double) event.getX() / (double) mWidth;
            double relativeY = (double) event.getY() / (double) mHeight;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mListener.down(relativeX, relativeY);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int batchSize = event.getHistorySize();
                    for (int i = 0; i < batchSize; i++) {
                        double historicalRelativeX = event.getHistoricalX(i) / (double) mWidth;
                        double historicalRelativeY = event.getHistoricalY(i) / (double) mHeight;
                        mListener.move(historicalRelativeX, historicalRelativeY);
                    }
                    mListener.move(relativeX, relativeY);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mListener.up();
                    return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void setListener(XYInputPadListener listener) {
        mListener = listener;
    }

    public void setDivisions(int columns, int rows) {
        mColumns = columns;
        mRows = rows;
    }

    public interface XYInputPadListener {
        void down(double relativeX, double relativeY);

        void move(double relativeX, double relativeY);

        void up();
    }
}
