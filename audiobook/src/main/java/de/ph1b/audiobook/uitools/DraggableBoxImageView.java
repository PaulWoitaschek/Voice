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

/**
 * ImageView that has a draggable square box and can return the position of the box
 */
public class DraggableBoxImageView extends ImageView {

    private final Paint borderLinePaint;
    //where the finger last went down
    private final PointF lastTouchPoint = new PointF();
    private final RectF dragRect = new RectF();
    private final RectF imageViewBounds = new RectF();

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
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - lastTouchPoint.x;
                float deltaY = y - lastTouchPoint.y;
                lastTouchPoint.set(x, y);

                if ((dragRect.right + deltaX) > imageViewBounds.right) {
                    dragRect.offsetTo(imageViewBounds.right - dragRect.width(), dragRect.top);
                } else if ((dragRect.left + deltaX) < 0) {
                    dragRect.offsetTo(0, dragRect.top);
                } else {
                    dragRect.offset(deltaX, 0);
                }

                if ((dragRect.bottom + deltaY) > imageViewBounds.bottom) {
                    dragRect.offsetTo(dragRect.left, imageViewBounds.bottom - dragRect.height());
                } else if ((dragRect.top + deltaY) < 0) {
                    dragRect.offsetTo(dragRect.left, 0);
                } else {
                    dragRect.offset(0, deltaY);
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

        // resets values
        lastTouchPoint.set(0, 0);
        imageViewBounds.set(0, 0, w, h);
        int min = Math.min(w, h);
        dragRect.set(0, 0, min, min);
    }

    /**
     * Calculates the position of the chosen cropped rect.
     *
     * @return the rect selection
     */
    public Rect getSelectedRect() {
        Drawable d = getDrawable();
        float widthScaleFactor = d.getIntrinsicWidth() / imageViewBounds.width();
        float heightScaleFactor = d.getIntrinsicHeight() / imageViewBounds.height();

        //returning the actual sizes
        int realLeft = Math.round(dragRect.left * widthScaleFactor);
        int realTop = Math.round(dragRect.top * heightScaleFactor);
        int realRight = Math.round(dragRect.right * widthScaleFactor);
        int realBottom = Math.round(dragRect.bottom * heightScaleFactor);

        return new Rect(realLeft, realTop, realRight, realBottom);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (!imageViewBounds.isEmpty()) {
            float proportion = imageViewBounds.height() / imageViewBounds.width();
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
