package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.AnyRes
import android.support.annotation.AttrRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDelegate
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import de.ph1b.audiobook.R

fun NumberPicker.theme() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        val colorAccent = ContextCompat.getColor(context, R.color.accent)
        var i = 0
        val count = childCount
        while (i < count) {
            val child = getChildAt(i)
            try {
                val selectorWheelPaintField = javaClass.getDeclaredField("mSelectorWheelPaint")
                selectorWheelPaintField.isAccessible = true
                (selectorWheelPaintField.get(this) as Paint).color = colorAccent
                (child as EditText?)?.setTextColor(colorAccent)
                invalidate()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

            try {
                val f1 = Class.forName("android.widget.NumberPicker").getDeclaredField("mSelectionDivider")
                f1.isAccessible = true
                val dividerDrawable = DrawableCompat.wrap(f1.get(this) as Drawable)
                DrawableCompat.setTint(dividerDrawable, colorAccent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            i++
        }
        invalidate()
    }
}

fun View.setVisible() {
    visibility = View.VISIBLE
}

fun View.setInvisible() {
    visibility = View.INVISIBLE
}

object ThemeUtil {

    @AnyRes
    fun getResourceId(c: Context, @AttrRes attr: Int): Int {
        val ta = c.obtainStyledAttributes(intArrayOf(attr))
        val resId = ta.getResourceId(0, -1)
        ta.recycle()
        if (resId == -1) {
            throw IllegalArgumentException("Resource with attr=$attr not found")
        }
        return resId
    }

    enum class Theme(@StringRes val nameId: Int, @AppCompatDelegate.NightMode val nightMode: Int) {
        DAY_NIGHT (R.string.pref_theme_daynight, AppCompatDelegate.MODE_NIGHT_AUTO),
        DAY(R.string.pref_theme_day, AppCompatDelegate.MODE_NIGHT_NO),
        NIGHT(R.string.pref_theme_night, AppCompatDelegate.MODE_NIGHT_YES)
    }
}
