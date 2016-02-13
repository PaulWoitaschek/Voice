/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject


class CoverReplacement(private val text: String, c: Context) : Drawable() {
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Align.CENTER
    }
    private val backgroundColor: Int

    @Inject internal lateinit var prefsManager: PrefsManager

    init {
        App.component().inject(this)

        check(text.isNotEmpty())

        // background
        backgroundColor = ContextCompat.getColor(c, prefsManager.theme.colorId)
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
        textPaint.setColorFilter(cf)
    }

    override fun getOpacity(): Int {
        return textPaint.alpha
    }
}
