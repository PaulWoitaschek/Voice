package de.ph1b.audiobook.misc

import android.graphics.Bitmap
import com.squareup.picasso.RequestCreator
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

fun RequestCreator.getOnUiThread(): Bitmap? {
  val latch = CountDownLatch(1)
  var result: Bitmap? = null
  thread {
    try {
      result = get()
    } catch (e: IOException) {
      Timber.e(e, "Error while getting result")
    }
    latch.countDown()
  }
  latch.await()
  return result
}
