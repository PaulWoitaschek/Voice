package de.ph1b.audiobook.misc

import android.os.Build
import android.os.StrictMode

object StrictModeInit {

  fun init() {
    StrictMode.setThreadPolicy(threadPolicy())
    StrictMode.setVmPolicy(vmPolicy())
  }

  private fun vmPolicy(): StrictMode.VmPolicy {
    val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      vmPolicyBuilder.detectFileUriExposure()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      vmPolicyBuilder.detectCleartextNetwork()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vmPolicyBuilder.detectContentUriWithoutPermission()
    }
    return vmPolicyBuilder
        .detectActivityLeaks()
        .detectLeakedClosableObjects()
        .detectLeakedRegistrationObjects()
        .detectLeakedSqlLiteObjects()
        .penaltyLog()
        .build()
  }

  private fun threadPolicy(): StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder()
      .detectAll()
      .penaltyLog()
      .build()
}
