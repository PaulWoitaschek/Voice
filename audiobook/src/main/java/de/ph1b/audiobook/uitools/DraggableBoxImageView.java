package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

import de.ph1b.audiobook.R;

public class DraggableBoxImageView extends ImageView {

    private final Paint borderLinePaint;

    private float left;
    private float right;
    private float top;
    private float bottom;

    private float imageViewWidth;
    private float imageViewHeight;

    private float maxWidth;
    private float maxHeight;

    //where the finger last went down
    private float fingerX;
    private float fingerY;

    public DraggableBoxImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int strokeWidth = getContext().getResources().getDimensionPixelSize(R.dimen.cover_edit_stroke_width);

        borderLinePaint = new Paint();
        borderLinePaint.setColor(getResources().getColor(ThemeUtil.getResourceId(getContext(), R.attr.colorAccent)));
        borderLinePaint.setStyle(Paint.Style.STROKE);
        borderLinePaint.setStrokeWidth(strokeWidth);
    }

    public DraggableBoxImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableBoxImageView(Context context) {
        this(context, null);
    }

    private void resetValues() {
        left = 0;
        right = 0;
        top = 0;
        bottom = 0;

        imageViewWidth = 0;
        imageViewHeight = 0;

        maxWidth = 0;
        maxHeight = 0;

        //where the finger last went down
        fingerX = 0;
        fingerY = 0;
    }

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
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {

        resetValues();

        imageViewWidth = w;
        imageViewHeight = h;

        if (getDrawable() != null && w > 0 && h > 0) {

            //setting frame accordingly
            if (w < h) {
                right = w;
                bottom = w;
            } else {
                right = h;
                bottom = h;
            }

            maxWidth = right - left;
            maxHeight = bottom - top;
        }

        super.onSizeChanged(w, h, oldW, oldH);
    }

    public Rect getCropPosition() {
        Drawable d = getDrawable();
        int origW = d.getIntrinsicWidth();
        int origH = d.getIntrinsicHeight();

        //returning the actual sizes
        int realLeft = Math.round(left / imageViewWidth * origW);
        int realTop = Math.round(top / imageViewHeight * origH);
        int realRight = Math.round(right / imageViewWidth * origW);
        int realBottom = Math.round(bottom / imageViewHeight * origH);

        return new Rect(realLeft, realTop, realRight, realBottom);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (imageViewHeight > 0 && imageViewWidth > 0) {
            float proportion = imageViewHeight / imageViewWidth;
            // only draw frame if relation doesn't already fit approx
            if (proportion < 0.95 || proportion > 1.05) {
                canvas.drawARGB(70, 0, 0, 0);
                float strokeSizeInDp = getResources().getDimension(R.dimen.cover_edit_stroke_width);
                float strokeSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, strokeSizeInDp, Resources.getSystem().getDisplayMetrics());
                float halfStrokeSize = strokeSize / 2;
                canvas.drawRect(left + halfStrokeSize, top + halfStrokeSize,
                        right - halfStrokeSize, bottom - halfStrokeSize, borderLinePaint);
            }
        }
    }
}
