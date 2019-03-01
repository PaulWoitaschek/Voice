package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import de.ph1b.audiobook.misc.dpToPxRounded
import kotlin.properties.Delegates

class ProgressPercentageView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
  private val radiusToTextScale = 0.4F
  private val radius = context.dpToPxRounded(32F)
  private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintForeground = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
  private val boundsText = Rect()

  var colorBackground: Int by Delegates.observable(Color.DKGRAY) { _, old, new ->
    if (old != new) {
      paintBackground.color = new
      paintBackground.alpha = alpha
      invalidate()
    }
  }

  var colorForeground: Int by Delegates.observable(Color.BLACK) { _, old, new ->
    if (old != new) {
      paintForeground.color = new
      paintForeground.alpha = alpha
      invalidate()
    }
  }

  var colorText: Int by Delegates.observable(Color.WHITE) { _, old, new ->
    if (old != new) {
      paintText.color = new
      invalidate()
    }
  }

  var textSize: Float by Delegates.observable(radiusToTextScale*radius) { _, old, new ->
    if (old != new) {
      paintText.textSize = new;
    }
  }

  var alpha: Int by Delegates.observable(175) { _, old, new ->
    if (old != new) {
      paintBackground.alpha = new;
      paintForeground.alpha = new;
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
      colorBackground = Color.BLUE
      colorForeground = Color.YELLOW
      progress = 0.33F
    } else {
      paintBackground.color = colorBackground
      paintBackground.alpha = alpha
      paintForeground.color = colorForeground
      paintForeground.alpha = alpha
      paintText.color = colorText
      paintText.textSize = textSize
      paintText.textAlign = Paint.Align.CENTER
    }
  }

  override fun onDraw(canvas: Canvas) {
    if (progress > 0F && progress < 1) {
      canvas.drawCircle((radius / 2).toFloat(), (radius / 2).toFloat(), (radius / 2).toFloat(), paintBackground)
      canvas.drawArc(radius.toFloat()*0.025F, radius.toFloat()*0.025F, radius.toFloat()*0.975F, radius.toFloat()*0.975F, 270F, 360F * progress, true, paintForeground)
      val text: String = String.format("%d%%", (progress*100).toInt())
      paintText.getTextBounds(text, 0, text.length, boundsText)
      canvas.drawText(String.format("%d%%", (progress*100).toInt()), radius.toFloat()/2, radius.toFloat()/2 - boundsText.exactCenterY(), paintText)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(radius, radius)
  }

}
