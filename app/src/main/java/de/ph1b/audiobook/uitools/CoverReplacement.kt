package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.drawable.Drawable
import android.text.TextPaint
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.color

class CoverReplacement(private val text: String, context: Context) : Drawable() {

  private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textAlign = Align.CENTER
  }
  private val backgroundColor = context.color(R.color.placeholderCoverColor)

  init {
    require(text.isNotEmpty())
  }

  override fun draw(canvas: Canvas) {
    val height = bounds.height()
    val width = bounds.width()

    textPaint.textSize = 2f * width / 3f

    canvas.drawColor(backgroundColor)
    val y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(text, 0, 1, width / 2f, y, textPaint)
  }

  override fun setAlpha(alpha: Int) {
    textPaint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    textPaint.colorFilter = cf
  }

  override fun getOpacity() = textPaint.alpha
}
