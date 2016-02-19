package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import de.ph1b.audiobook.R

/**
 * ImageView that has a draggable square box and can return the position of the box
 */
class DraggableBoxImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0) : ImageView(context, attrs, defStyle) {

    private val borderLinePaint: Paint
    //where the finger last went down
    private val lastTouchPoint = PointF()
    private val dragRect = RectF()
    private val imageViewBounds = RectF()

    init {
        val strokeWidth = getContext().resources.getDimensionPixelSize(R.dimen.cover_edit_stroke_width)

        borderLinePaint = Paint()
        borderLinePaint.color = ContextCompat.getColor(getContext(), ThemeUtil.getResourceId(getContext(), R.attr.colorAccent))
        borderLinePaint.style = Paint.Style.STROKE
        borderLinePaint.strokeWidth = strokeWidth.toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val x = event.x
        val y = event.y

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPoint.set(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - lastTouchPoint.x
                val deltaY = y - lastTouchPoint.y
                lastTouchPoint.set(x, y)

                if ((dragRect.right + deltaX) > imageViewBounds.right) {
                    dragRect.offsetTo(imageViewBounds.right - dragRect.width(), dragRect.top)
                } else if ((dragRect.left + deltaX) < 0) {
                    dragRect.offsetTo(0f, dragRect.top)
                } else {
                    dragRect.offset(deltaX, 0f)
                }

                if ((dragRect.bottom + deltaY) > imageViewBounds.bottom) {
                    dragRect.offsetTo(dragRect.left, imageViewBounds.bottom - dragRect.height())
                } else if ((dragRect.top + deltaY) < 0) {
                    dragRect.offsetTo(dragRect.left, 0f)
                } else {
                    dragRect.offset(0f, deltaY)
                }

                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                lastTouchPoint.set(0f, 0f)
                return true
            }
            else -> return false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)

        // resets values
        lastTouchPoint.set(0f, 0f)
        imageViewBounds.set(0f, 0f, w.toFloat(), h.toFloat())
        val min = Math.min(w, h)
        dragRect.set(0f, 0f, min.toFloat(), min.toFloat())
    }

    /**
     * Calculates the position of the chosen cropped rect.

     * @return the rect selection
     */
    //returning the actual sizes
    val selectedRect: Rect
        get() {
            val d = drawable
            val widthScaleFactor = d.intrinsicWidth / imageViewBounds.width()
            val heightScaleFactor = d.intrinsicHeight / imageViewBounds.height()
            val realLeft = Math.round(dragRect.left * widthScaleFactor)
            val realTop = Math.round(dragRect.top * heightScaleFactor)
            val realRight = Math.round(dragRect.right * widthScaleFactor)
            val realBottom = Math.round(dragRect.bottom * heightScaleFactor)

            return Rect(realLeft, realTop, realRight, realBottom)
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!imageViewBounds.isEmpty) {
            val proportion = imageViewBounds.height() / imageViewBounds.width()
            // only draw frame if relation doesn't already fit approx
            if (proportion < 0.95 || proportion > 1.05) {
                canvas.drawARGB(70, 0, 0, 0)
                val halfStrokeSize = borderLinePaint.strokeWidth / 2
                canvas.drawRect(dragRect.left + halfStrokeSize, dragRect.top + halfStrokeSize,
                        dragRect.right - halfStrokeSize, dragRect.bottom - halfStrokeSize, borderLinePaint)
            }
        }
    }
}
