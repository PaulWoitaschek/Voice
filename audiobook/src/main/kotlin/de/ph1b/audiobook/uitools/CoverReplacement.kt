package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import com.google.common.base.Preconditions
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject


class CoverReplacement(private val text: String, c: Context) : Drawable() {
    private val textPaint: Paint
    private val backgroundColor: Int

    @Inject internal lateinit var prefsManager: PrefsManager

    init {
        App.component().inject(this)

        Preconditions.checkArgument(text.isNotEmpty())

        // text
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Align.CENTER

        // background
        backgroundColor = ContextCompat.getColor(c, prefsManager.theme.colorId)
    }

    override fun draw(canvas: Canvas) {
        val rect = bounds
        val height = rect.height()
        val width = rect.width()

        textPaint.textSize = 2f * width / 3f

        canvas.drawColor(backgroundColor)
        val y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, 0, 1, width / 2f, y, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        textPaint.setColorFilter(cf)
    }

    override fun getOpacity(): Int {
        return textPaint.alpha
    }
}
