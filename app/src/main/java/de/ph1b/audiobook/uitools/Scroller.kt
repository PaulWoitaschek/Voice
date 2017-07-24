package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.dpToPx
import de.ph1b.audiobook.misc.dpToPxRounded
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * A scrollbar for the recycler view
 */
class Scroller @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

  private var yPosition = 0F
  private var attachedTo: RecyclerView? = null
  private var dragging = false
  private var shown = true
  private val hideAfterTimeout = PublishSubject.create<Unit>()
  private val interpolator = FastOutLinearInInterpolator()
  private val thumbWidth = context.dpToPx(6F)

  private val thumbRect = RectF()
  private val backgroundRect = RectF()
  private val backgroundPaint = Paint().apply {
    color = context.color(R.color.scroller_background)
  }
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

      showAndRefreshTimeout()
    }
  }

  init {
    hideAfterTimeout.debounce(1500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
        .subscribe {
          show(false)
        }
  }

  fun attachTo(recyclerView: RecyclerView) {
    attachedTo?.removeOnScrollListener(scrollListener)
    recyclerView.addOnScrollListener(scrollListener)
    attachedTo = recyclerView
  }

  fun show(show: Boolean) {
    if (this.shown == show) {
      return
    }
    this.shown = show

    if (show) {
      animate().cancel()
      translationX = 0F
    } else {
      animate().translationX(thumbWidth)
          .setInterpolator(interpolator)
          .setDuration(250)
          .start()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    val measuredHeight = measuredHeight.toFloat()
    val measuredWidth = measuredWidth.toFloat()

    backgroundRect.set(measuredWidth - thumbWidth, 0F, measuredWidth, measuredHeight)
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawRect(backgroundRect, backgroundPaint)

    val measuredHeight = measuredHeight.toFloat()
    val measuredWidth = measuredWidth.toFloat()

    val centerY = yPosition * measuredHeight
    val barTop = centerY - scrollerHeight / 2F
    val barBottom = centerY + scrollerHeight / 2F
    thumbRect.set(measuredWidth - thumbWidth, barTop, measuredWidth, barBottom)

    if (thumbRect.top < 0) thumbRect.inset(0F, thumbRect.top)
    if (thumbRect.bottom > measuredHeight) thumbRect.inset(0F, measuredHeight - thumbRect.bottom)

    canvas.drawRect(thumbRect, thumbPaint)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    showAndRefreshTimeout()

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
              .round()
          if (position >= 0) recycler.smoothScrollToPosition(position)

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

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val width = context.dpToPxRounded(12F)
    setMeasuredDimension(width, measuredHeight)
  }

  private fun showAndRefreshTimeout() {
    show(true)
    hideAfterTimeout.onNext(Unit)
  }

  private fun Float.round() = Math.round(this)
}
