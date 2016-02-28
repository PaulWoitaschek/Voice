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

package de.ph1b.audiobook.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.webkit.WebView

/**
 * WebView that is able to take a screenshot.
 *
 * @author: Paul Woitaschek
 */
class ScreenShotWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : WebView(context, attrs, defStyleAttr) {

    fun takeScreenshot(cropRect: Rect): Bitmap {
        val w = Math.max(computeHorizontalScrollRange(), width)
        val full = Bitmap.createBitmap(w, contentHeight, Bitmap.Config.ARGB_8888);
        val c = Canvas(full)
        draw(c)

        val cropped = Bitmap.createBitmap(full, cropRect.left + scrollX, cropRect.top + scrollY, cropRect.width(), cropRect.height())
        full.recycle()

        return cropped
    }
}