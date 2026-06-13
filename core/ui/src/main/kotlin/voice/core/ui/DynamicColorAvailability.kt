package voice.core.ui

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import dev.zacsweers.metro.Inject

@Inject
class DynamicColorAvailability {

  @ChecksSdkIntAtLeast(api = 31)
  fun isSupported(): Boolean {
    return Build.VERSION.SDK_INT >= 31
  }
}
