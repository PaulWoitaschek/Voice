package de.ph1b.audiobook.uitools;/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Property;


/**
 * A class for creating simple transformation buttons. It is very simple to
 * use and perfectly fits simple Material icons' transformation.
 *
 * @author Artem Chepurnoy
 */
public class PlayPauseDrawable extends Drawable {


    /**
     * Pause icon
     */
    private static final float[][] VERTEX_PAUSE = {
            {10f, 6f, 6f, 10f, 10f, 14f, 14f, 18f, 18f},
            {5f, 5f, 19f, 19f, 5f, 5f, 19f, 19f, 5f}
    };

    /**
     * Play icon
     */
    private static final float[][] VERTEX_PLAY = {
            {19f, 19f, 8f, 8f, 19f, 19f, 8f, 8f, 19f},
            {12f, 12f, 5f, 9f, 12f, 12f, 5f, 19f, 12f}
    };
    private final static Property<PlayPauseDrawable, Float> TRANSFORM =
            new FloatProperty<PlayPauseDrawable>() {
                @Override
                public void setValue(PlayPauseDrawable object, float value) {
                    object.setTransformation(value);
                }

                @Override
                public Float get(PlayPauseDrawable object) {
                    return object.getTransformation();
                }
            };
    private final Animator mAnimator = ObjectAnimator.ofFloat(this, TRANSFORM, 0f, 1f);
    private final Path mPath;
    private final Paint mPaint;
    private final float[][][] mVertex;
    private float mProgress;
    private int mFromShape;
    private int mToShape;
    public PlayPauseDrawable() {
        this(VERTEX_PAUSE, VERTEX_PLAY);
    }

    private PlayPauseDrawable(@NonNull float[][]... vertex) {
        mVertex = vertex;

        mPath = new Path();
        mPath.setFillType(Path.FillType.WINDING);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void transformToPause(boolean animated) {
        transformToShape(0, animated);
    }

    public void transformToPlay(boolean animated) {
        transformToShape(1, animated);
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    private void transformToShape(int i, boolean animated) {
        if (mToShape == i) {
            // Otherwise this will not be animated.
            return;
        }
        if (animated) {
            mAnimator.setDuration(300);
        } else {
            mAnimator.setDuration(0);
        }

        setTransformationTarget(i);
        mAnimator.cancel();
        mAnimator.start();
    }

    private void setTransformationTarget(int i) {
        mFromShape = mToShape;
        mToShape = i;
    }

    private float getTransformation() {
        return mProgress;
    }

    private void setTransformation(float progress) {
        mProgress = progress;
        Rect rect = getBounds();

        final float size = Math.min(rect.right - rect.left, rect.bottom - rect.top);
        final float left = rect.left + (rect.right - rect.left - size) / 2;
        final float top = rect.top + (rect.bottom - rect.top - size) / 2;

        mPath.reset();
        mPath.moveTo(
                left + calcTransformation(0, 0, progress, size),
                top + calcTransformation(1, 0, progress, size));
        for (int i = 1; i < mVertex[0][0].length; i++) {
            mPath.lineTo(
                    left + calcTransformation(0, i, progress, size),
                    top + calcTransformation(1, i, progress, size));
        }

        mPath.close();
        invalidateSelf();
    }

    private float calcTransformation(int type, int i, float progress, float size) {
        float v0 = mVertex[mFromShape][type][i] * (1f - progress);
        float v1 = mVertex[mToShape][type][i] * progress;
        return (v0 + v1) * size / 24f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        setTransformation(mProgress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
