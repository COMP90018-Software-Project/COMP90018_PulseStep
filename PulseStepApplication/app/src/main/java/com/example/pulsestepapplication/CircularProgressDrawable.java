package com.example.pulsestepapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;

public class CircularProgressDrawable extends Drawable {
    private Paint paint;
    private float sweepAngle = 0;
    private RectF oval;
    private int strokeWidth = 20;

    public CircularProgressDrawable(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        oval = new RectF();
    }

    @Override
    public void draw(Canvas canvas) {
        float startAngle = -90;
        canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        oval.set(left + strokeWidth, top + strokeWidth, right - strokeWidth, bottom - strokeWidth);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    public void setProgress(float progress) {
        sweepAngle = 360 * progress / 100;
        invalidateSelf();  // Redraw
    }

    public void setColor(int color) {
        paint.setColor(color);
    }
}
