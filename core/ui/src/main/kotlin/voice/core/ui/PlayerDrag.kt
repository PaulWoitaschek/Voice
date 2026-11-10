@file:Suppress("ComposeCompositionLocalUsage")

package voice.core.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// with 3-button navigation, Android 16 sends back previews without a swipe edge too, so the
// edge can't tell them apart from a drag
@Stable
class PlayerDrag {
  var active by mutableStateOf(false)
}

val LocalPlayerDrag = staticCompositionLocalOf { PlayerDrag() }
