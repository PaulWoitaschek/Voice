@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.sleepTimer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import voice.core.ui.icons.VoiceIcons
import kotlin.time.Duration.Companion.milliseconds
import voice.core.strings.R as StringsR

private val PRESETS = listOf(5, 15, 30, 60)

@Composable
fun SleepTimerDialog(
  viewState: SleepTimerViewState,
  onDismiss: () -> Unit,
  onIncrementSleepTime: () -> Unit,
  onDecrementSleepTime: () -> Unit,
  onAcceptSleepTime: (Int) -> Unit,
  onAcceptSleepAtEndOfChapter: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismiss,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = StringsR.string.sleep_timer_dialog_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(24.dp))
      val presetShapes = listOf(
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie9Sided,
        MaterialShapes.SoftBurst,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        PRESETS.forEachIndexed { index, time ->
          PresetButton(
            minutes = time,
            polygon = presetShapes[index],
            containerColor = if (index % 2 == 0) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.tertiaryContainer
            },
            contentColor = if (index % 2 == 0) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onTertiaryContainer
            },
            onClick = { onAcceptSleepTime(time) },
          )
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Row(
          modifier = Modifier.padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ContinuousPressIcon(
            onEmit = onDecrementSleepTime,
            icon = VoiceIcons.Remove,
            contentDescription = stringResource(id = StringsR.string.sleep_timer_dialog_action_decrement),
          )
          AnimatedContent(
            modifier = Modifier.weight(1F),
            targetState = viewState.customSleepTime,
            transitionSpec = {
              val up = targetState > initialState
              val enter = slideInVertically { height -> if (up) height else -height } + fadeIn()
              val exit = slideOutVertically { height -> if (up) -height else height } + fadeOut()
              (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "customSleepTime",
          ) { time ->
            Text(
              modifier = Modifier.fillMaxWidth(),
              text = minutes(minutes = time),
              style = MaterialTheme.typography.titleLargeEmphasized,
              textAlign = TextAlign.Center,
            )
          }
          ContinuousPressIcon(
            onEmit = onIncrementSleepTime,
            icon = VoiceIcons.Add,
            contentDescription = stringResource(id = StringsR.string.sleep_timer_dialog_action_increment),
          )
          FilledIconButton(
            onClick = { onAcceptSleepTime(viewState.customSleepTime) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
          ) {
            Icon(
              imageVector = VoiceIcons.Check,
              contentDescription = minutes(minutes = viewState.customSleepTime),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      FilledTonalButton(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        onClick = onAcceptSleepAtEndOfChapter,
        shapes = ButtonDefaults.shapes(),
      ) {
        Icon(imageVector = VoiceIcons.Bedtime, contentDescription = null)
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(id = StringsR.string.sleep_timer_end_of_chapter))
      }
    }
  }
}

/** A preset drawn in a playful Material shape that twists and squishes when pressed. */
@Composable
private fun PresetButton(
  minutes: Int,
  polygon: RoundedPolygon,
  containerColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val rotation by animateFloatAsState(
    targetValue = if (pressed) 60F else 0F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMediumLow),
    label = "presetRotation",
  )
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.88F else 1F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium),
    label = "presetScale",
  )
  val shape = polygon.toShape()
  val description = minutes(minutes = minutes)
  Box(
    modifier = modifier
      .size(76.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Button,
        onClick = onClick,
      )
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(76.dp)
        .graphicsLayer { rotationZ = rotation }
        .background(containerColor, shape),
    )
    Column(
      modifier = Modifier.clearAndSetSemantics {},
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = minutes.toString(),
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = contentColor,
      )
      Text(
        text = stringResource(StringsR.string.sleep_timer_dialog_minutes_short),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
      )
    }
  }
}

@Composable
private fun ContinuousPressIcon(
  onEmit: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  var isPressed by remember { mutableStateOf(false) }

  LaunchedEffect(isPressed, onEmit) {
    if (isPressed) {
      delay(500.milliseconds)
      while (isPressed) {
        onEmit()
        delay(100.milliseconds)
      }
    }
  }
  val interactionSource = remember { MutableInteractionSource() }
  Icon(
    imageVector = icon,
    contentDescription = contentDescription,
    modifier = modifier
      .size(48.dp)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = false),
        onClick = onEmit,
        onLongClick = { isPressed = true },
      )
      .padding(12.dp),
  )
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Release ||
        interaction is PressInteraction.Cancel
      ) {
        isPressed = false
      }
    }
  }
}

@Composable
@ReadOnlyComposable
private fun minutes(minutes: Int): String {
  return pluralStringResource(StringsR.plurals.duration_minutes, minutes, minutes)
}
