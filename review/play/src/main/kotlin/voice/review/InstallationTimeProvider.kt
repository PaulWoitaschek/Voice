package voice.review

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.zacsweers.metro.Inject
import java.time.Instant

@Inject
class InstallationTimeProvider(private val context: Context) {

  internal fun installationTime(): Instant {
    val packageManager = context.packageManager

    val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
      packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
      packageManager.getPackageInfo(context.packageName, 0)
    }
    return Instant.ofEpochMilli(packageInfo.firstInstallTime)
  }
}
