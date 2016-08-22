package de.ph1b.audiobook.misc

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.f2prateek.rx.preferences.Preference

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

fun Fragment.setupActionbar(toolbar: Toolbar? = null,
                            homeAsUpEnabled: Boolean? = null,
                            @DrawableRes upIndicator: Int? = null,
                            showTitle: Boolean? = false,
                            @StringRes titleRes: Int? = null,
                            title: String? = null) =
        (activity as AppCompatActivity).setupActionbar(toolbar, homeAsUpEnabled, upIndicator, showTitle, titleRes, title)

fun AppCompatActivity.setupActionbar(toolbar: Toolbar? = null,
                                     homeAsUpEnabled: Boolean? = null,
                                     @DrawableRes upIndicator: Int? = null,
                                     showTitle: Boolean? = false,
                                     @StringRes titleRes: Int? = null,
                                     title: String? = null) {
    if (toolbar != null) setSupportActionBar(toolbar)

    val actionBar = supportActionBar!!

    if (homeAsUpEnabled != null) actionBar.setDisplayHomeAsUpEnabled(homeAsUpEnabled)

    if (upIndicator != null) actionBar.setHomeAsUpIndicator(upIndicator)

    if (titleRes != null) actionBar.setTitle(titleRes)
    if (title != null) actionBar.title = title

    if (showTitle != null) actionBar.setDisplayShowTitleEnabled(showTitle)
}