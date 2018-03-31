package de.ph1b.audiobook.features.imagepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.misc.layoutInflater
import timber.log.Timber

/**
 * Layout that enables a crop selection. Put this on top of over another view.
 */
class CropOverlay @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(
  context,
  attrs,
  defStyleAttr
) {

  private val leftCircle = newCircle()
  private val topCircle = newCircle()
  private val rightCircle = newCircle()
  private val bottomCircle = newCircle()
  private val lastTouchPoint = PointF()
  private val dragRectCache = RectF()
  private val dragRect = RectF()
  private val bounds = RectF()
  private val darkeningPaint = Paint().apply {
    setARGB(120, 0, 0, 0)
  }

  init {
    setWillNotDraw(false)

    addView(leftCircle)
    addView(topCircle)
    addView(rightCircle)
    addView(bottomCircle)
  }

  private val scaleGestureDetector = ScaleGestureDetector(
    context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      override fun onScale(detector: ScaleGestureDetector): Boolean {
        val dx = detector.currentSpanX - detector.previousSpanX
        val dy = detector.currentSpanY - detector.previousSpanY
        val max = Math.max(dx, dy)
        dragRect.squareInset(-max)
        return max != 0f
      }
    }
  )

  var selectionOn = false
    set(value) {
      if (value != field) {
        field = value

        leftCircle.isVisible = value
        rightCircle.isVisible = value
        topCircle.isVisible = value
        bottomCircle.isVisible = value

        invalidate()
      }
    }

  private var eventType: EventType? = null
  private var resizeType: Resize? = null
  private val touchOffset = context.dpToPxRounded(16F)

  private fun newCircle() =
    context.layoutInflater().inflate(R.layout.circle, this@CropOverlay, false).apply {
      isVisible = false
    }

  private fun minRectSize() = Math.min(bounds.width(), bounds.height()) / 3f

  private infix fun Float.inRangeOf(target: Float) =
    this >= (target - touchOffset) && this <= (target + touchOffset)

  private fun MotionEvent.asResizeType(): Resize? {
    val x = x
    val y = y
    val rect = dragRect

    return when {
      x inRangeOf rect.left -> Resize.LEFT
      x inRangeOf rect.right -> Resize.RIGHT
      y inRangeOf rect.top -> Resize.TOP
      y inRangeOf rect.bottom -> Resize.BOTTOM
      else -> null
    }
  }

  private fun RectF.squareInset(value: Float) {
    inset(value, value)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!selectionOn) return super.onTouchEvent(event)

    // use the cache rect to detect changes
    dragRectCache.set(dragRect)

    scaleGestureDetector.onTouchEvent(event)
    val gestureDetectorIsHandling = scaleGestureDetector.isInProgress
    Timber.i("Gesture detector is handling=$gestureDetectorIsHandling")
    if (gestureDetectorIsHandling) {
      // pinch handles this
      resizeType = null
      eventType = null
      lastTouchPoint.set(0f, 0f)
    } else {
      val action = event.action
      val x = event.x
      val y = event.y

      when (action) {
        MotionEvent.ACTION_DOWN -> {
          // prepare operation
          resizeType = event.asResizeType()
          when {
            resizeType != null -> {
              eventType = EventType.RESIZE
              lastTouchPoint.set(x, y)
            }
            dragRect.contains(x, y) -> {
              lastTouchPoint.set(x, y)
              eventType = EventType.DRAG
            }
            else -> eventType = null
          }
        }
        MotionEvent.ACTION_MOVE -> {
          val deltaX = x - lastTouchPoint.x
          val deltaY = y - lastTouchPoint.y
          lastTouchPoint.set(x, y)

          if (eventType == EventType.DRAG) {
            // just offset by drag
            dragRect.offset(deltaX, deltaY)
          } else if (eventType == EventType.RESIZE) {
            // resize depending on which side touched
            val inset = when (resizeType!!) {
              Resize.TOP -> y - dragRect.top
              Resize.RIGHT -> dragRect.right - x
              Resize.BOTTOM -> dragRect.bottom - y
              Resize.LEFT -> x - dragRect.left
            }
            Timber.i("inset=$inset, resizeType=$resizeType, dragRect=$dragRect, x=$x, y=$y")
            dragRect.squareInset(inset)
          }

        }
        MotionEvent.ACTION_UP -> {
          // reset
          lastTouchPoint.set(0f, 0f)
        }
      }
    }

    // make sure the drag rect sits perfect
    preserveSize()
    preserveBounds()
    // only invalidate if there are changes
    if (dragRect != dragRectCache) invalidate()
    return true
  }

  private fun preserveBounds() {
    val rightDiff = dragRect.right - bounds.right
    if (rightDiff > 0) {
      dragRect.offset(-rightDiff, 0f)
    }

    val leftDiff = dragRect.left - bounds.left
    if (leftDiff < 0) {
      dragRect.offset(-leftDiff, 0f)
    }

    val topDiff = dragRect.top - bounds.top
    if (topDiff < 0) {
      dragRect.offset(0f, -topDiff)
    }

    val bottomDiff = dragRect.bottom - bounds.bottom
    if (bottomDiff > 0) {
      dragRect.offset(0f, -bottomDiff)
    }
  }

  private fun preserveSize() {
    val circleSize = bottomCircle.width

    // preserve min size
    val minSize = minRectSize()
    val w = dragRect.width()
    if (w < minSize) {
      val diff = minSize - w
      dragRect.squareInset(-diff / 2f)
      Timber.i("preserving min size with diff=$diff")
    }

    // preserve max size
    val dragW = dragRect.width()
    val boundsSize = Math.min(bounds.width(), bounds.height()) - circleSize
    val diff = dragW - boundsSize
    if (diff > 0) {
      dragRect.squareInset(diff / 2f)
      Timber.i("preserve max size, insetting with diff=$diff, dragW=$dragW, boundsSize=$boundsSize")
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
    super.onSizeChanged(w, h, oldW, oldH)

    // resets values
    lastTouchPoint.set(0f, 0f)
    val wf = w.toFloat()
    val hf = h.toFloat()
    bounds.set(0f, 0f, wf, hf)
    val dragSize = Math.min(wf, hf) / 2f

    dragRect.set(0f, 0f, dragSize, dragSize)
    dragRect.offset(bounds.centerX() - dragSize / 2f, bounds.centerY() - dragSize / 2f)
  }

  /**
   * Calculates the position of the chosen cropped rect.
   * @return the rect selection
   */
  val selectedRect: Rect
    get() {
      val widthScaleFactor = 1
      val heightScaleFactor = 1
      val realLeft = Math.round(dragRect.left * widthScaleFactor)
      val realTop = Math.round(dragRect.top * heightScaleFactor)
      val realRight = Math.round(dragRect.right * widthScaleFactor)
      val realBottom = Math.round(dragRect.bottom * heightScaleFactor)

      return Rect(realLeft, realTop, realRight, realBottom)
    }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (!selectionOn) return

    if (!bounds.isEmpty) {
      val boundsHeight = bounds.height()
      val boundsWidth = bounds.width()

      val left = dragRect.left
      val top = dragRect.top
      val right = dragRect.right
      val bottom = dragRect.bottom
      val centerX = left + dragRect.width() / 2f
      val centerY = top + dragRect.height() / 2f

      canvas.drawRect(0f, 0f, left, boundsHeight, darkeningPaint) // left
      canvas.drawRect(left, 0f, right, top, darkeningPaint) // top
      canvas.drawRect(right, 0f, boundsWidth, boundsHeight, darkeningPaint) // right
      canvas.drawRect(left, bottom, right, boundsHeight, darkeningPaint) // bottom

      with(canvas) {
        topCircle.center(centerX, top)
        leftCircle.center(left, centerY)
        rightCircle.center(right, centerY)
        bottomCircle.center(centerX, bottom)
      }
    }
  }

  private fun View.center(x: Float, y: Float) {
    translationX = x - width / 2f
    translationY = y - height / 2f
  }

  enum class EventType {
    DRAG,
    RESIZE
  }

  enum class Resize {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT
  }
}
