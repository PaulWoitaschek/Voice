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

package de.ph1b.audiobook.misc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView


val Fragment.actionBar: ActionBar
    get() = (activity as AppCompatActivity).supportActionBar!!

inline fun <reified T : Activity> Activity.startActivity(args: Bundle? = null, flags: Int? = null) {
    val intent = Intent(this, T::class.java)
    args?.let { intent.putExtras(args) }
    flags?.let { intent.flags = flags }
    startActivity(intent)
}

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun Context.drawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(this, id)

@ColorInt fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun View.layoutInflater() = context.layoutInflater()

fun SeekBar.onProgressChanged(progressChanged: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            progressChanged(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })
}

fun Context.dpToPx(dp: Int) = Math.round(TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics))

fun Drawable.tinted(@ColorInt color: Int): Drawable {
    val wrapped = DrawableCompat.wrap(this)
    DrawableCompat.setTint(wrapped, color)
    return wrapped
}

fun TextView.leftCompoundDrawable(): Drawable? = compoundDrawables[0]
fun TextView.topCompoundDrawable(): Drawable? = compoundDrawables[1]
fun TextView.rightCompoundDrawable(): Drawable? = compoundDrawables[2]
fun TextView.bottomCompoundDrawable(): Drawable? = compoundDrawables[3]

