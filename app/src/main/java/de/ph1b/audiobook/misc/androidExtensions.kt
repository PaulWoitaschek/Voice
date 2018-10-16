package de.ph1b.audiobook.misc

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.f2prateek.rx.preferences2.Preference
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.injection.App
import java.io.File
import java.io.FileFilter

fun Context.layoutInflater(): LayoutInflater = LayoutInflater.from(this)

fun Context.drawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(this, id)!!

@ColorInt
fun Context.color(@ColorRes id: Int): Int {
  return ContextCompat.getColor(this, id)
}

fun View.layoutInflater() = context.layoutInflater()

/** enforce a non-null property */
var <T> Preference<T>.value: T
  get() = get()!!
  set(value) = set(value!!)

fun Context.dpToPx(dp: Float) =
  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPxRounded(dp: Float) = Math.round(dpToPx(dp))

fun Drawable.tinted(@ColorInt color: Int): Drawable {
  val wrapped = DrawableCompat.wrap(mutate())
  wrapped.setTint(color)
  return wrapped
}

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

suspend fun Book.coverFile(): File = coverFile(App.component.context)

fun LayoutInflater.inflate(@LayoutRes resource: Int): View = inflate(resource, null, false)
