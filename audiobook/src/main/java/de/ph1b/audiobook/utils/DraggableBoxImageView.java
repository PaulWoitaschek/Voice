package de.ph1b.audiobook.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;


public class DraggableBoxImageView extends ImageView {

    private Paint borderLinePaint;

    private float left = 0;
    private float right = 0;
    private float top = 0;
    private float bottom = 0;

    private float imageViewWidth = 0;
    private float imageViewHeight = 0;

    private float maxWidth;
    private float maxHeight;

    //where the finger last went down
    private float fingerX = 0;
    private float fingerY = 0;

    private float relationX = 0;
    private float relationY = 0;


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                fingerX = x;
                fingerY = y;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - fingerX;
                float deltaY = y - fingerY;
                fingerX = x;
                fingerY = y;

                if ((right + deltaX) > imageViewWidth) {
                    right = imageViewWidth;
                    left = imageViewWidth - maxWidth;
                } else if ((left + deltaX) < 0) {
                    left = 0;
                    right = maxWidth;
                } else {
                    right += deltaX;
                    left += deltaX;
                }

                if ((bottom + deltaY) > imageViewHeight) {
                    bottom = imageViewHeight;
                    top = imageViewHeight - maxHeight;
                } else if ((top + deltaY) < 0) {
                    top = 0;
                    bottom = maxHeight;
                } else {
                    bottom += deltaY;
                    top += deltaY;
                }

                if (BuildConfig.DEBUG)
                    Log.d("dbim", "move!" + String.valueOf(deltaX) + ":" + String.valueOf(deltaY));

                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                fingerX = 0;
                fingerY = 0;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        relationX = getResources().getDimension(R.dimen.thumb_size_x);
        relationY = getResources().getDimension(R.dimen.thumb_size_y);

        imageViewWidth = w;
        imageViewHeight = h;

        if (getDrawable() != null && w > 0 && h > 0) {
            if ((h / relationY) > (w / relationX)) {
                //if this is the case, then for the desired ration, the image is too high. so set max to width
                right = w;
                bottom = right * relationY / relationX;

            } else {
                //..image is to width, so set max to height
                bottom = h;
                right = bottom * relationX / relationY;
            }

            maxWidth = right - left;
            maxHeight = bottom - top;

            Log.d("measured", String.valueOf(left) + "/" + String.valueOf(top) + "/" +
                    String.valueOf(right) + "/" +
                    String.valueOf(bottom));
        }

        super.onSizeChanged(w, h, oldw, oldh);
    }

    public Rect getCropPosition() {
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        float scaleX = f[Matrix.MSCALE_X];
        float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        Drawable d = getDrawable();
        int origW = d.getIntrinsicWidth();
        int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        float actW = origW * scaleX;
        float actH = origH * scaleY;

        //returning the actual sizes
        int realLeft = Math.round(left / maxWidth * actW);
        int realTop = Math.round(top / maxHeight * actH);
        int realRight = Math.round(right / maxWidth * actW);
        int realBottom = Math.round(bottom / maxHeight * actH);

        return new Rect(realLeft, realTop, realRight, realBottom);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (imageViewHeight > 0 && imageViewWidth > 0 && relationX > 0 && relationY > 0) {
            float difference = Math.abs(imageViewHeight / imageViewWidth) / (relationX / relationY);
            // only draw frame if relation doesn't already fit approx
            if (difference < 0.95 || difference > 1.05) {
                canvas.drawARGB(70, 0, 0, 0);
                float halfStrokeSize = CommonTasks.convertDpToPx(getResources().getDimension(R.dimen.cover_edit_stroke_width)) / 2;
                canvas.drawRect(left + halfStrokeSize, top + halfStrokeSize,
                        right - halfStrokeSize, bottom - halfStrokeSize, borderLinePaint);
            }
        }
    }

    //constructor!
    @SuppressWarnings("UnusedDeclaration")
    public DraggableBoxImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    //constructor!
    @SuppressWarnings("UnusedDeclaration")
    public DraggableBoxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    //constructor!
    @SuppressWarnings("UnusedDeclaration")
    public DraggableBoxImageView(Context context) {
        super(context);
        init();
    }

    private void init() {
        float strokeWidth = CommonTasks.convertDpToPx(getResources().getDimension(R.dimen.cover_edit_stroke_width));

        borderLinePaint = new Paint();
        borderLinePaint.setColor(getResources().getColor(R.color.primary));
        borderLinePaint.setStyle(Paint.Style.STROKE);
        borderLinePaint.setStrokeWidth(strokeWidth);
    }
}
