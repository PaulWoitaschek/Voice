package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import de.ph1b.audiobook.R;

public class DraggableBoxImageView extends ImageView {

    private final Paint borderLinePaint;
    //where the finger last went down
    private final PointF lastTouchPoint = new PointF();
    private final RectF dragRect = new RectF();
    private float imageViewWidth;
    private float imageViewHeight;

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

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - lastTouchPoint.x;
                float deltaY = y - lastTouchPoint.y;
                lastTouchPoint.set(x, y);

                if ((dragRect.right + deltaX) > imageViewWidth) {
                    dragRect.right = imageViewWidth;
                    dragRect.left = imageViewWidth - dragRect.width();
                } else if ((dragRect.left + deltaX) < 0) {
                    dragRect.left = 0;
                    dragRect.right = dragRect.width();
                } else {
                    dragRect.right += deltaX;
                    dragRect.left += deltaX;
                }

                if ((dragRect.bottom + deltaY) > imageViewHeight) {
                    dragRect.bottom = imageViewHeight;
                    dragRect.top = imageViewHeight - dragRect.height();
                } else if ((dragRect.top + deltaY) < 0) {
                    dragRect.top = 0;
                    dragRect.bottom = dragRect.height();
                } else {
                    dragRect.bottom += deltaY;
                    dragRect.top += deltaY;
                }

                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                lastTouchPoint.set(0, 0);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        dragRect.set(0, 0, 0, 0);


        //where the finger last went down
        lastTouchPoint.set(0, 0);

        imageViewWidth = w;
        imageViewHeight = h;

        if (getDrawable() != null && w > 0 && h > 0) {

            //setting frame accordingly
            if (w < h) {
                dragRect.right = w;
                dragRect.bottom = w;
            } else {
                dragRect.right = h;
                dragRect.bottom = h;
            }
        }
    }

    public Rect getCropPosition() {
        Drawable d = getDrawable();
        int origW = d.getIntrinsicWidth();
        int origH = d.getIntrinsicHeight();

        //returning the actual sizes
        int realLeft = Math.round(dragRect.left / imageViewWidth * origW);
        int realTop = Math.round(dragRect.top / imageViewHeight * origH);
        int realRight = Math.round(dragRect.right / imageViewWidth * origW);
        int realBottom = Math.round(dragRect.bottom / imageViewHeight * origH);

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
                float halfStrokeSize = borderLinePaint.getStrokeWidth() / 2;
                canvas.drawRect(dragRect.left + halfStrokeSize, dragRect.top + halfStrokeSize,
                        dragRect.right - halfStrokeSize, dragRect.bottom - halfStrokeSize, borderLinePaint);
            }
        }
    }
}
