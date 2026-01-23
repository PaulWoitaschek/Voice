package voice.features.playbackScreen.batteryOptimization

import android.app.Application
import android.os.PowerManager
import androidx.core.content.getSystemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

fun interface IsIgnoringBatteryOptimizations {
  operator fun invoke(): Boolean
}

@ContributesBinding(AppScope::class)
class IsIgnoringBatteryOptimizationsImpl(private val context: Application) : IsIgnoringBatteryOptimizations {
  override fun invoke(): Boolean {
    val powerManager = context.getSystemService<PowerManager>()
      ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }
}
