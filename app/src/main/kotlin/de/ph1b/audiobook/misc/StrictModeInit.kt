package de.ph1b.audiobook.misc

import android.os.Build
import android.os.StrictMode

object StrictModeInit {

  fun init() {
    StrictMode.setThreadPolicy(threadPolicy())
    StrictMode.setVmPolicy(vmPolicy())
  }

  private fun vmPolicy(): StrictMode.VmPolicy = StrictMode.VmPolicy.Builder()
    .detectActivityLeaks()
    .detectLeakedClosableObjects()
    .detectLeakedRegistrationObjects()
    .detectFileUriExposure()
    .detectCleartextNetwork()
    .apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        detectContentUriWithoutPermission()
        detectUntaggedSockets()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        detectCredentialProtectedWhileLocked()
      }
    }
    .penaltyLog()
    .build()

  private fun threadPolicy(): StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder()
    .detectAll()
    .penaltyLog()
    .penaltyFlashScreen()
    .build()
}
