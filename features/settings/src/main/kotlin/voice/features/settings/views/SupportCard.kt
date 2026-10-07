@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import voice.core.strings.R as StringsR

/** Asks for a donation, with a heart beating in a slowly turning burst. */
@Composable
internal fun SupportCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clock = rememberAnimationClock(running = true)
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = IslandShape,
    color = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary,
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          Modifier
            .matchParentSize()
            .graphicsLayer { rotationZ = clock.value * 15F }
            .background(MaterialTheme.colorScheme.onTertiary, MaterialShapes.SoftBurst.toShape()),
        )
        Icon(
          modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
              val scale = 1F + 0.15F * heartbeat(clock.value)
              scaleX = scale
              scaleY = scale
            },
          imageVector = VoiceIcons.Favorite,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
        )
      }
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(StringsR.string.settings_support_support_voice_title),
          style = MaterialTheme.typography.titleLargeEmphasized,
        )
        Text(
          text = stringResource(StringsR.string.settings_support_support_voice_summary),
          style = MaterialTheme.typography.bodyMedium,
          color = LocalContentColor.current.copy(alpha = 0.85F),
        )
      }
      Chevron()
    }
  }
}

/** Two quick beats, then a rest. */
private fun heartbeat(seconds: Float): Float {
  val phase = seconds % 1.4F
  fun beat(start: Float): Float {
    val local = (phase - start) / 0.16F
    return if (local in 0F..1F) sin(local * PI.toFloat()) else 0F
  }
  return max(beat(0F), beat(0.24F) * 0.7F)
}
