package de.ph1b.audiobook.misc

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Looper
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import com.f2prateek.rx.preferences.Preference
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

fun Context.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
fun Context.dpToPxRounded(dp: Float) = Math.round(dpToPx(dp))

fun Drawable.tinted(@ColorInt color: Int): Drawable {
  val wrapped = DrawableCompat.wrap(this)
  DrawableCompat.setTint(wrapped, color)
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

/** find a callback. The hosting activity must implement [RouterProvider] and the supplied key must match to the instance id of a controller */
fun <T> DialogFragment.findCallback(controllerBundleKey: String): T {
  val routerProvider = activity as RouterProvider
  val router = routerProvider.provideRouter()
  val controllerId: String = arguments!!.getString(controllerBundleKey)
  @Suppress("UNCHECKED_CAST")
  return router.getControllerWithInstanceId(controllerId) as T
}

inline fun View.onFirstPreDraw(crossinline action: () -> Unit) {
  viewTreeObserver.addOnPreDrawListener(
      object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          viewTreeObserver.removeOnPreDrawListener(this)
          action()
          return true
        }
      }
  )
}

fun checkMainThread() {
  check(Looper.getMainLooper() == Looper.myLooper()) {
    "Is not on ui thread!"
  }
}

fun Book.coverFile(): File = coverFile(App.component.context)
