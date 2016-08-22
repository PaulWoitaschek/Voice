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
import com.afollestad.materialdialogs.MaterialDialog
import com.f2prateek.rx.preferences.Preference


val Fragment.actionBar: ActionBar
    get() = (activity as AppCompatActivity).supportActionBar!!

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun Context.drawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(this, id)

@ColorInt fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun View.layoutInflater() = context.layoutInflater()

fun MaterialDialog.Builder.positiveClicked(listener: () -> Unit): MaterialDialog.Builder {
    onPositive { dialog, which -> listener() }
    return this
}

/** same as get() but force cast to non null **/
fun <T> Preference<T>.value() = get()!!

fun Context.dpToPx(dp: Int) = Math.round(TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics))

fun Drawable.tinted(@ColorInt color: Int): Drawable {
    val wrapped = DrawableCompat.wrap(this)
    DrawableCompat.setTint(wrapped, color)
    return wrapped
}