package de.ph1b.audiobook.crashreporting

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import io.fabric.sdk.android.Fabric

object CrashReporter {

  fun log(message: String) {
    Crashlytics.log(message)
  }

  fun logException(throwable: Throwable) {
    Crashlytics.logException(throwable)
  }

  fun init(app: Application) {
    val crashlytics = Crashlytics.Builder()
      .core(
        CrashlyticsCore.Builder()
          .disabled(BuildConfig.DEBUG)
          .build()
      )
      .build()
    Fabric.with(app, crashlytics)
  }
}
