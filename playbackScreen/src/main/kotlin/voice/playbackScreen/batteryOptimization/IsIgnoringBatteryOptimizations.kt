package voice.playbackScreen.batteryOptimization

import android.app.Application
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.squareup.anvil.annotations.ContributesBinding
import voice.common.AppScope
import javax.inject.Inject

fun interface IsIgnoringBatteryOptimizations {
  operator fun invoke(): Boolean
}

@ContributesBinding(AppScope::class)
class IsIgnoringBatteryOptimizationsImpl
@Inject constructor(private val context: Application) : IsIgnoringBatteryOptimizations {
  override fun invoke(): Boolean {
    val powerManager = context.getSystemService<PowerManager>()
      ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }
}
