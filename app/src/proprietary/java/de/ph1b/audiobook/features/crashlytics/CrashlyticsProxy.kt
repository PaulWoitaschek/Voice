package de.ph1b.audiobook.features.crashlytics

import android.annotation.SuppressLint
import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.misc.ErrorReporter
import de.ph1b.audiobook.persistence.pref.Pref
import io.fabric.sdk.android.Fabric

/**
 * Proxy-class that forwards to crashlytics
 */
object CrashlyticsProxy : ErrorReporter {

  private var enabled = false

  override fun log(message: String) {
    if (enabled) {
      Crashlytics.log(message)
    }
  }

  override fun logException(throwable: Throwable) {
    if (enabled) {
      Crashlytics.logException(throwable)
    }
  }

  @SuppressLint("CheckResult")
  fun init(app: Application, enabled: Pref<Boolean>) {
    enabled.stream
      .distinctUntilChanged()
      .subscribe { updateEnabledState(it, app) }

  }

  private fun updateEnabledState(enabled: Boolean, app: Application) {
    this.enabled = enabled
    val crashlytics = Crashlytics.Builder()
      .core(
        CrashlyticsCore.Builder()
          .disabled(BuildConfig.DEBUG || enabled)
          .build()
      )
      .build()
    Fabric.with(app, crashlytics)
  }
}
