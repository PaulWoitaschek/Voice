package voice.playbackScreen.batteryOptimization

import android.app.Application
import android.os.PowerManager
import androidx.core.content.getSystemService
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.common.AppScope

fun interface IsIgnoringBatteryOptimizations {
  operator fun invoke(): Boolean
}

@ContributesBinding(AppScope::class)
@Inject
class IsIgnoringBatteryOptimizationsImpl(private val context: Application) : IsIgnoringBatteryOptimizations {
  override fun invoke(): Boolean {
    val powerManager = context.getSystemService<PowerManager>()
      ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }
}
