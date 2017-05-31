package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.dpToPx

/**
 * A scrollbar for the recycler view
 *
 * @author Paul Woitaschek
 */
class Scroller @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

  private var yPosition = 0F
  private var attachedTo: RecyclerView? = null
  private var dragging = false

  private val thumbRect = RectF()
  private val scrollerBackgroundColor = context.color(R.color.scroller_background)
  private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = context.color(R.color.accent)
  }
  private val scrollerHeight = context.dpToPx(64F)

  private val scrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      if (dragging) return

      val verticalScrollOffset = recyclerView.computeVerticalScrollOffset()
      val verticalScrollRange = recyclerView.computeVerticalScrollRange()
      yPosition = verticalScrollOffset.toFloat() / (verticalScrollRange - height)
      invalidate()
    }
  }

  fun attachTo(recyclerView: RecyclerView) {
    attachedTo?.removeOnScrollListener(scrollListener)
    recyclerView.addOnScrollListener(scrollListener)
    attachedTo = recyclerView
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawColor(scrollerBackgroundColor)

    val centerY = yPosition * measuredHeight
    val barTop = centerY - scrollerHeight / 2F
    val barBottom = centerY + scrollerHeight / 2F
    thumbRect.set(0F, barTop, measuredWidth.toFloat(), barBottom)

    if (thumbRect.top < 0) thumbRect.inset(0F, thumbRect.top)
    if (thumbRect.bottom > measuredHeight) thumbRect.inset(0F, measuredHeight - thumbRect.bottom)

    canvas.drawRect(thumbRect, thumbPaint)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        dragging = true
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        if (dragging) {
          val recycler = attachedTo
              ?: return true

          yPosition = event.y / measuredHeight.toFloat()
          invalidate()

          val position = recycler.layoutManager.itemCount * yPosition
          recycler.layoutManager.scrollToPosition(position.round())

          return true
        }
      }
      MotionEvent.ACTION_UP -> {
        dragging = false
        return true
      }
    }
    return super.onTouchEvent(event)
  }

  private fun Float.round() = Math.round(this)
}
