@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.SupporterBadgeIcon
import voice.core.ui.drawConfetti
import voice.core.ui.label
import voice.core.ui.rememberConfettiState
import voice.core.strings.R as StringsR

@Composable
internal fun ThankYou(
  badge: SupporterBadge?,
  onDone: () -> Unit,
) {
  val shown = badge ?: SupporterBadge.FirstCup
  val colors = MaterialTheme.colorScheme
  val confettiColors = listOf(colors.primary, colors.secondary, colors.tertiary, colors.primaryContainer, colors.tertiaryContainer)
  val confetti = rememberConfettiState()
  var celebrated by rememberSaveable { mutableStateOf(false) }
  val pop = remember { Animatable(if (celebrated) 1F else 0F) }
  val haptics = LocalHapticFeedback.current
  LaunchedEffect(Unit) {
    if (celebrated) return@LaunchedEffect
    celebrated = true
    delay(150)
    launch { pop.animateTo(1F, spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessLow)) }
    delay(150)
    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    confetti.burst()
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .size(160.dp)
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
      SupporterBadgeIcon(
        badge = shown,
        size = 112.dp,
        contentDescription = shown.label(),
        modifier = Modifier.graphicsLayer {
          scaleX = pop.value
          scaleY = pop.value
          rotationZ = (1F - pop.value) * -90F
        },
      )
    }
    Text(
      text = stringResource(StringsR.string.support_thank_you_title),
      style = MaterialTheme.typography.headlineMediumEmphasized,
      textAlign = TextAlign.Center,
    )
    Text(
      text = shown.label(),
      style = MaterialTheme.typography.labelLarge,
      color = colors.primary,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(StringsR.string.support_thank_you_message),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(StringsR.string.support_note_signature),
      style = MaterialTheme.typography.labelLarge,
      color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    val size = ButtonDefaults.MediumContainerHeight
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(size),
      onClick = onDone,
      contentPadding = ButtonDefaults.contentPaddingFor(size),
      shapes = ButtonDefaults.shapesFor(size),
    ) {
      Text(
        text = stringResource(StringsR.string.support_thank_you_action),
        style = ButtonDefaults.textStyleFor(size),
      )
    }
  }
}
