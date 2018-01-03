package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import de.ph1b.audiobook.misc.dpToPxRounded
import kotlin.properties.Delegates

class SquareProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  private val barHeight = context.dpToPxRounded(4F)
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

  var color: Int by Delegates.observable(Color.TRANSPARENT) { _, old, new ->
    if (old != new) {
      paint.color = new
      invalidate()
    }
  }

  var progress: Float by Delegates.observable(0F) { _, old, new ->
    require(new in 0F..1F) {
      "Progress $new must be in [0,1]"
    }
    if (old != new) {
      invalidate()
    }
  }

  init {
    if (isInEditMode) {
      color = Color.BLUE
      progress = 0.33F
    }
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawRect(0F, 0F, measuredWidth * progress, measuredHeight.toFloat(), paint)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(measuredWidth, barHeight)
  }
}
