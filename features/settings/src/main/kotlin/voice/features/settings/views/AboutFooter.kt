@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.ui.ConfettiState
import voice.core.ui.drawConfetti
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import voice.core.strings.R as StringsR

/**
 * The app version below a slowly turning play badge. Tapping squishes and spins the badge. Unlocking
 * the developer menu (the 13th tap) is celebrated with [confetti] bursting out of it.
 */
@Composable
internal fun AboutFooter(
  appVersion: String,
  confetti: ConfettiState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clock = rememberAnimationClock(running = true)
  val scope = rememberCoroutineScope()
  val spin = remember { Animatable(0F) }
  val squish = remember { Animatable(1F) }
  val colors = MaterialTheme.colorScheme
  val confettiColors = listOf(colors.primary, colors.secondary, colors.tertiary, colors.primaryContainer, colors.tertiaryContainer)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        // without a clipping ripple, so the confetti can fly everywhere. The squish is the feedback.
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          onClick()
          scope.launch {
            spin.animateTo(spin.value + 90F, spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessLow))
          }
          scope.launch {
            squish.snapTo(0.8F)
            squish.animateTo(1F, spring(dampingRatio = 0.3F, stiffness = Spring.StiffnessMedium))
          }
        }
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .drawWithContent {
            drawContent()
            drawConfetti(
              state = confetti,
              center = center,
              radius = size.minDimension / 2,
              colors = confettiColors,
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(
          Modifier
            .matchParentSize()
            .graphicsLayer {
              rotationZ = clock.value * 8F + spin.value
              scaleX = squish.value
              scaleY = squish.value
            }
            .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
        )
        Icon(
          modifier = Modifier.size(32.dp),
          imageVector = VoiceIcons.PlayArrow,
          contentDescription = null,
          tint = colors.onPrimary,
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.settings_about_app_version_title),
        style = MaterialTheme.typography.labelLarge,
        color = LocalContentColor.current.copy(alpha = 0.7F),
      )
      Text(
        text = appVersion,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalContentColor.current.copy(alpha = 0.7F),
      )
    }
  }
}
