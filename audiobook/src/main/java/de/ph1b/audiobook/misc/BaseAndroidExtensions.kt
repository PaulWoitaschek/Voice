package de.ph1b.audiobook.misc

import android.content.Context
import android.graphics.drawable.Drawable
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
import com.afollestad.materialdialogs.MaterialDialog


val Fragment.actionBar: ActionBar
    get() = (activity as AppCompatActivity).supportActionBar!!

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun Context.drawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(this, id)

@ColorInt fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun View.layoutInflater() = context.layoutInflater()

fun SeekBar.onProgressChanged(initialNotification: Boolean = false, progressChanged: (Int) -> Unit) {
    val listener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            progressChanged(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    }

    setOnSeekBarChangeListener(listener)
    if (initialNotification) listener.onProgressChanged(this, progress, false)
}

fun MaterialDialog.Builder.positiveClicked(listener: () -> Unit): MaterialDialog.Builder {
    onPositive { dialog, which -> listener() }
    return this
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

