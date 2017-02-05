package de.ph1b.audiobook.playback

import android.support.test.InstrumentationRegistry
import java.io.File
import java.util.*


inline fun onMainThreadSync(crossinline action: () -> Unit) {
  InstrumentationRegistry.getInstrumentation().runOnMainSync { action() }
}

fun prepareTestFiles(): List<File> {
  val files = ArrayList<File>()
  val instrumentationContext = InstrumentationRegistry.getContext()
  val testFolder = File(InstrumentationRegistry.getTargetContext().filesDir, "testFolder")
  testFolder.mkdirs()
  instrumentationContext.assets.list("samples").forEach { asset ->
    val out = File(testFolder, asset)
    out.outputStream().use { outputStream ->
      instrumentationContext.assets.open("samples/$asset").use { inputStream ->
        inputStream.copyTo(outputStream)
      }
    }
    files.add(out)
  }
  return files
}