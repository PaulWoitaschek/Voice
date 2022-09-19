package voice.playbackSpeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackSpeedController(speed: Float, onValueChange: (Float) -> Unit) {
  Box(modifier = Modifier
    .background(MaterialTheme.colorScheme.background)
    .padding(vertical = 16.dp)) {
    PlaybackSpeedComponent(
      inputValue = speed,
      selectedColor = MaterialTheme.colorScheme.primary,
      unselectedColor = MaterialTheme.colorScheme.onSurface,
      onValueChange = onValueChange,
    )
  }
}
