package voice.features.cover.crop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import voice.core.ui.dpToPxRounded
import voice.features.cover.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class CropOverlay @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : FrameLayout(
  context,
  attrs,
  defStyleAttr,
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

  private val scaleGestureDetector = ScaleGestureDetector(
    context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      override fun onScale(detector: ScaleGestureDetector): Boolean {
        val dx = detector.currentSpanX - detector.previousSpanX
        val dy = detector.currentSpanY - detector.previousSpanY
        val max = max(dx, dy)
        dragRect.squareInset(-max)
        return max != 0f
      }
    },
  )

  var selectionOn: Boolean by Delegates.observable(false) { _, old, new ->
    if (old != new) {
      updateForSelectionState()
    }
  }

  init {
    setWillNotDraw(false)

    addView(leftCircle)
    addView(topCircle)
    addView(rightCircle)
    addView(bottomCircle)

    updateForSelectionState()
  }

  private fun updateForSelectionState() {
    leftCircle.isVisible = selectionOn
    rightCircle.isVisible = selectionOn
    topCircle.isVisible = selectionOn
    bottomCircle.isVisible = selectionOn

    invalidate()
  }

  private var eventType: EventType? = null
  private var resizeType: Resize? = null
  private val touchOffset = context.dpToPxRounded(16F)

  private fun newCircle() = LayoutInflater.from(context)
    .inflate(R.layout.circle, this@CropOverlay, false).apply {
      isVisible = false
    }

  private fun minRectSize() = min(bounds.width(), bounds.height()) / 3f

  private infix fun Float.inRangeOf(target: Float) = this >= (target - touchOffset) && this <= (target + touchOffset)

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

    dragRectCache.set(dragRect)

    scaleGestureDetector.onTouchEvent(event)
    val gestureDetectorIsHandling = scaleGestureDetector.isInProgress
    if (gestureDetectorIsHandling) {
      resizeType = null
      eventType = null
      lastTouchPoint.set(0f, 0f)
    } else {
      val action = event.action
      val x = event.x
      val y = event.y

      when (action) {
        MotionEvent.ACTION_DOWN -> {
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
            dragRect.offset(deltaX, deltaY)
          } else if (eventType == EventType.RESIZE) {
            val inset = when (resizeType!!) {
              Resize.TOP -> y - dragRect.top
              Resize.RIGHT -> dragRect.right - x
              Resize.BOTTOM -> dragRect.bottom - y
              Resize.LEFT -> x - dragRect.left
            }
            dragRect.squareInset(inset)
          }
        }
        MotionEvent.ACTION_UP -> {
          lastTouchPoint.set(0f, 0f)
        }
      }
    }

    preserveSize()
    preserveBounds()
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

    val minSize = minRectSize()
    val w = dragRect.width()
    if (w < minSize) {
      val diff = minSize - w
      dragRect.squareInset(-diff / 2f)
    }

    val dragW = dragRect.width()
    val boundsSize = min(bounds.width(), bounds.height()) - circleSize
    val diff = dragW - boundsSize
    if (diff > 0) {
      dragRect.squareInset(diff / 2f)
    }
  }

  override fun onSizeChanged(
    w: Int,
    h: Int,
    oldW: Int,
    oldH: Int,
  ) {
    super.onSizeChanged(w, h, oldW, oldH)

    lastTouchPoint.set(0f, 0f)
    val wf = w.toFloat()
    val hf = h.toFloat()
    bounds.set(0f, 0f, wf, hf)
    val dragSize = min(wf, hf)

    dragRect.set(0f, 0f, dragSize, dragSize)
    dragRect.offset(bounds.centerX() - dragSize / 2f, bounds.centerY() - dragSize / 2f)
  }

  val selectedRect: Rect
    get() {
      val widthScaleFactor = 1
      val heightScaleFactor = 1
      val realLeft = (dragRect.left * widthScaleFactor).roundToInt()
      val realTop = (dragRect.top * heightScaleFactor).roundToInt()
      val realRight = (dragRect.right * widthScaleFactor).roundToInt()
      val realBottom = (dragRect.bottom * heightScaleFactor).roundToInt()

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

      canvas.drawRect(0f, 0f, left, boundsHeight, darkeningPaint)
      canvas.drawRect(left, 0f, right, top, darkeningPaint)
      canvas.drawRect(right, 0f, boundsWidth, boundsHeight, darkeningPaint)
      canvas.drawRect(left, bottom, right, boundsHeight, darkeningPaint)

      topCircle.center(centerX, top)
      leftCircle.center(left, centerY)
      rightCircle.center(right, centerY)
      bottomCircle.center(centerX, bottom)
    }
  }

  private fun View.center(
    x: Float,
    y: Float,
  ) {
    translationX = x - width / 2f
    translationY = y - height / 2f
  }

  enum class EventType {
    DRAG,
    RESIZE,
  }

  enum class Resize {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
  }
}
