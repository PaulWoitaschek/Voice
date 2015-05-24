package de.ph1b.audiobook.uitools;
/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 * Modified by Paul Woitaschek <woitaschek@posteo.de>
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
    private final Animator animator = ObjectAnimator.ofFloat(this, TRANSFORM, 0f, 1f);
    private final Path path;
    private final Paint paint;
    private final float[][][] vertex;
    private float progress;
    private int fromShape;
    private int toShape;
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

    public PlayPauseDrawable() {
        this(VERTEX_PAUSE, VERTEX_PLAY);
    }

    private PlayPauseDrawable(@NonNull float[][]... vertex) {
        this.vertex = vertex;

        path = new Path();
        path.setFillType(Path.FillType.WINDING);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
    }

    public void transformToPause(boolean animated) {
        transformToShape(0, animated);
    }

    public void transformToPlay(boolean animated) {
        transformToShape(1, animated);
    }

    /**
     * public void setColor(int color) {
     * paint.setColor(color);
     * invalidateSelf();
     * }*
     */

    private void transformToShape(int i, boolean animated) {
        // Otherwise this will not be animated.
        if (toShape != i) {
            if (animated) {
                animator.setDuration(300);
            } else {
                animator.setDuration(0);
            }

            setTransformationTarget(i);
            animator.cancel();
            animator.start();
        }
    }

    private void setTransformationTarget(int i) {
        fromShape = toShape;
        toShape = i;
    }

    private float getTransformation() {
        return progress;
    }

    private void setTransformation(float progress) {
        this.progress = progress;
        Rect rect = getBounds();

        final float size = Math.min(rect.right - rect.left, rect.bottom - rect.top);
        final float left = rect.left + (rect.right - rect.left - size) / 2;
        final float top = rect.top + (rect.bottom - rect.top - size) / 2;

        path.reset();
        path.moveTo(
                left + calcTransformation(0, 0, progress, size),
                top + calcTransformation(1, 0, progress, size));
        for (int i = 1; i < vertex[0][0].length; i++) {
            path.lineTo(
                    left + calcTransformation(0, i, progress, size),
                    top + calcTransformation(1, i, progress, size));
        }

        path.close();
        invalidateSelf();
    }

    private float calcTransformation(int type, int i, float progress, float size) {
        float v0 = vertex[fromShape][type][i] * (1f - progress);
        float v1 = vertex[toShape][type][i] * progress;
        return (v0 + v1) * size / 24f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        setTransformation(progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
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
