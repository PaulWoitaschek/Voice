package voice.app.misc

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import dev.zacsweers.metro.Inject
import voice.app.BuildConfig
import voice.core.common.AppInfoProvider
import kotlin.time.Instant

@Inject
class AppInfoProviderImpl(private val application: Application) : AppInfoProvider {
  override val versionName: String = BuildConfig.VERSION_NAME
  override val analyticsIncluded: Boolean = BuildConfig.INCLUDE_ANALYTICS
  override val supportDevelopmentIncluded: Boolean = BuildConfig.SUPPORT_DEVELOPMENT_INCLUDED
  override val installTime: Instant by lazy {
    val packageManager = application.packageManager
    val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
      packageManager.getPackageInfo(application.packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
      packageManager.getPackageInfo(application.packageName, 0)
    }
    Instant.fromEpochMilliseconds(packageInfo.firstInstallTime)
  }
}
