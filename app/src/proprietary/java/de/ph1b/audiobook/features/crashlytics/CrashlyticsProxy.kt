package de.ph1b.audiobook.features.crashlytics

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.common.ErrorReporter
import io.fabric.sdk.android.Fabric

/**
 * Proxy-class that forwards to crashlytics
 */
object CrashlyticsProxy : ErrorReporter {

  override fun log(priority: Int, tag: String?, message: String?) {
    Crashlytics.log(priority, tag, message)
  }

  override fun logException(throwable: Throwable) {
    Crashlytics.logException(throwable)
  }

  fun init(app: Application) {
    val crashlytics = Crashlytics.Builder()
        .core(CrashlyticsCore.Builder()
            .disabled(BuildConfig.DEBUG)
            .build())
        .build()
    Fabric.with(app, crashlytics)
  }
}
