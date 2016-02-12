package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.widget.EditText
import android.widget.NumberPicker
import de.ph1b.audiobook.R

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

    fun theme(numberPicker: NumberPicker) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val colorAccent = ContextCompat.getColor(numberPicker.context, R.color.accent)
            var i = 0
            val count = numberPicker.childCount
            while (i < count) {
                val child = numberPicker.getChildAt(i)
                try {
                    val selectorWheelPaintField = numberPicker.javaClass.getDeclaredField("mSelectorWheelPaint")
                    selectorWheelPaintField.isAccessible = true
                    (selectorWheelPaintField.get(numberPicker) as Paint).color = colorAccent
                    (child as EditText?)?.setTextColor(colorAccent)
                    numberPicker.invalidate()
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
                    val dividerDrawable = DrawableCompat.wrap(f1.get(numberPicker) as Drawable)
                    DrawableCompat.setTint(dividerDrawable, colorAccent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                i++
            }
            numberPicker.invalidate()
        }
    }

    enum class Theme internal constructor(@StyleRes val themeId: Int, @StringRes val nameId: Int, @ColorRes val colorId: Int) {
        LIGHT(R.style.LightTheme, R.string.pref_theme_light, R.color.light_primary_dark),
        DARK(R.style.DarkTheme, R.string.pref_theme_dark, R.color.dark_primary_dark)
    }
}
