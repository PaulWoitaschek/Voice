package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlaybackService
import org.acra.ACRA
import org.acra.annotation.ReportsCrashes
import org.acra.config.ConfigurationBuilder
import org.acra.sender.HttpSender.Method
import org.acra.sender.HttpSender.Type
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@ReportsCrashes(
  httpMethod = Method.PUT,
  reportType = Type.JSON,
  buildConfigClass = BuildConfig::class,
  formUri = "http://acra-f85814.smileupps.com/acra-myapp-0b5541/_design/acra-storage/_update/report",
  formUriBasicAuthLogin = "129user",
  formUriBasicAuthPassword = "IQykOJBswx7C7YtY")
class App : Application() {

  @Inject lateinit var bookAdder: BookAdder
  @Inject lateinit var prefsManager: PrefsManager

  override fun onCreate() {
    super.onCreate()

    // init acra + return early if this is the sender service
    if (!BuildConfig.DEBUG) {
      val isSenderProcess = ACRA.isACRASenderServiceProcess()
      if (isSenderProcess || Random().nextInt(5) == 0) {
        val config = ConfigurationBuilder(this)
          .build()
        ACRA.init(this, config)
      }
      if (isSenderProcess) return
    }

    applicationComponent = newComponent()
    component().inject(this)

    if (BuildConfig.DEBUG) {
      // init timber
      Timber.plant(Timber.DebugTree())
    }

    bookAdder.scanForFiles(true)
    startService(Intent(this, PlaybackService::class.java))

    AppCompatDelegate.setDefaultNightMode(prefsManager.theme.get()!!.nightMode)
  }

  private fun newComponent(): ApplicationComponent {
    return DaggerApplicationComponent.builder()
      .androidModule(AndroidModule(this))
      .build()
  }

  companion object {

    private var applicationComponent: ApplicationComponent? = null

    fun component(): ApplicationComponent {
      return applicationComponent!!
    }
  }
}