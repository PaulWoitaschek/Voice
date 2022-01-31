package de.ph1b.audiobook.misc

import android.content.Context
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.injection.appComponent
import java.io.File
import kotlin.math.roundToInt

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun View.layoutInflater() = context.layoutInflater()

fun Context.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPxRounded(dp: Float): Int = dpToPx(dp).roundToInt()

fun checkMainThread() {
  check(Looper.getMainLooper() == Looper.myLooper()) {
    "Is not on ui thread!"
  }
}

fun Book.coverFile(): File = coverFile(appComponent.context)

fun LayoutInflater.inflate(@LayoutRes resource: Int): View = inflate(resource, null, false)
