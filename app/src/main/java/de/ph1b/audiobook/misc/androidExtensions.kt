package de.ph1b.audiobook.misc

import android.content.Context
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.uitools.ThemeUtil
import java.io.File
import java.io.FileFilter
import kotlin.math.roundToInt

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

@ColorInt
fun Context.colorFromAttr(@AttrRes id: Int): Int {
  val colorRes = ThemeUtil.getResourceId(this, id)
  return getColor(colorRes)
}

fun View.layoutInflater() = context.layoutInflater()

fun Context.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPxRounded(dp: Float): Int = dpToPx(dp).roundToInt()

/**
 * As there are cases where [File.listFiles] returns null even though it is a directory, we return
 * an empty list instead.
 */
fun File.listFilesSafely(filter: FileFilter? = null): List<File> {
  val array: Array<File>? = if (filter == null) listFiles() else listFiles(filter)
  return array?.toList() ?: emptyList()
}

fun checkMainThread() {
  check(Looper.getMainLooper() == Looper.myLooper()) {
    "Is not on ui thread!"
  }
}

fun Book.coverFile(): File = coverFile(appComponent.context)

fun LayoutInflater.inflate(@LayoutRes resource: Int): View = inflate(resource, null, false)
