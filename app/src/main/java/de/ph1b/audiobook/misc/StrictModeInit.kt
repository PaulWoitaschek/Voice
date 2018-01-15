package de.ph1b.audiobook.misc

import android.os.StrictMode

object StrictModeInit {

  fun init() {
    StrictMode.setThreadPolicy(threadPolicy())
    StrictMode.setVmPolicy(vmPolicy())
  }

  private fun vmPolicy(): StrictMode.VmPolicy = StrictMode.VmPolicy.Builder()
      .detectAll()
      .penaltyLog()
      .build()

  private fun threadPolicy(): StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder()
      .detectAll()
      .penaltyLog()
      .penaltyFlashScreen()
      .build()
}
