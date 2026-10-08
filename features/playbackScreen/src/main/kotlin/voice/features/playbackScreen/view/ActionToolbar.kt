@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.strings.R
import voice.core.ui.formatTime
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState
import java.text.DecimalFormat

@Composable
internal fun ActionToolbar(
  sleepTimerState: BookPlayViewState.SleepTimerViewState,
  playbackSpeed: Float,
  skipSilence: Boolean,
  volumeBoostActive: Boolean,
  onSleepTimerClick: () -> Unit,
  onSpeedClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val speedFormat = remember { DecimalFormat("0.0#") }
  HorizontalFloatingToolbar(
    expanded = true,
    modifier = modifier,
    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
  ) {
    val sleepTimerActive = sleepTimerState is BookPlayViewState.SleepTimerViewState.Enabled
    ToolbarAction(
      active = sleepTimerActive,
      // a click cancels a running timer
      icon = if (sleepTimerActive) VoiceIcons.BedtimeOff else VoiceIcons.Bedtime,
      contentDescription = stringResource(R.string.sleep_timer_action_open),
      label = (sleepTimerState as? BookPlayViewState.SleepTimerViewState.Enabled.WithDuration)
        ?.let { formatTime(it.leftDuration.inWholeMilliseconds) },
      onClick = onSleepTimerClick,
    )
    val speedChanged = playbackSpeed !in 0.99F..1.01F
    ToolbarAction(
      active = speedChanged,
      icon = VoiceIcons.Speed,
      contentDescription = stringResource(R.string.playback_speed_title),
      label = if (speedChanged) speedFormat.format(playbackSpeed) + "×" else null,
      onClick = onSpeedClick,
    )
    BookmarkAction(
      onClick = onBookmarkClick,
      onLongClick = onBookmarkLongClick,
    )
    ToolbarAction(
      active = skipSilence,
      icon = VoiceIcons.ContentCut,
      contentDescription = stringResource(R.string.playback_option_skip_silence),
      onClick = onSkipSilenceClick,
      toggle = true,
      // the only option that doesn't open a titled screen, and its icon alone is ambiguous
      showTooltipOnClick = true,
    )
    ToolbarAction(
      active = volumeBoostActive,
      icon = VoiceIcons.VolumeUp,
      contentDescription = stringResource(R.string.playback_option_volume_boost),
      onClick = onVolumeBoostClick,
    )
  }
}

@Composable
private fun BookmarkAction(
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  var burstCount by remember { mutableIntStateOf(0) }
  val burst = remember { Animatable(1F) }
  LaunchedEffect(burstCount) {
    if (burstCount > 0) {
      burst.snapTo(0F)
      burst.animateTo(1F, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
  }
  val burstShape = MaterialShapes.SoftBurst.toShape()
  val burstColor = MaterialTheme.colorScheme.tertiary
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (burst.value < 1F) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .graphicsLayer {
            val p = burst.value
            scaleX = 0.4F + 1.2F * p
            scaleY = 0.4F + 1.2F * p
            rotationZ = 120F * p
            alpha = 1F - p
          }
          .background(burstColor, burstShape),
      )
    }
    ToolbarAction(
      active = burst.value < 0.5F,
      icon = if (burst.value < 1F) VoiceIcons.BookmarkAdd else VoiceIcons.CollectionsBookmark,
      contentDescription = stringResource(R.string.bookmark_title),
      onClick = onClick,
      onLongClick = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        burstCount++
        onLongClick()
      },
      onLongClickLabel = stringResource(R.string.bookmark_save_moment),
      selectable = false,
    )
  }
}

@Composable
private fun ToolbarAction(
  active: Boolean,
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  onLongClick: (() -> Unit)? = null,
  onLongClickLabel: String? = null,
  selectable: Boolean = true,
  toggle: Boolean = false,
  showTooltipOnClick: Boolean = false,
) {
  val scope = rememberCoroutineScope()
  val tooltipState = rememberTooltipState()
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(contentDescription) } },
    state = tooltipState,
    modifier = modifier,
    // a long click of its own takes precedence over showing the tooltip
    enableUserInput = onLongClick == null,
  ) {
    ToolbarActionContent(
      active = active,
      icon = icon,
      contentDescription = contentDescription,
      label = label,
      selectable = selectable,
      toggle = toggle,
      onClick = {
        onClick()
        if (showTooltipOnClick) {
          scope.launch { tooltipState.show() }
        }
      },
      onLongClick = onLongClick,
      onLongClickLabel = onLongClickLabel,
    )
  }
}

@Composable
private fun ToolbarActionContent(
  active: Boolean,
  icon: ImageVector,
  contentDescription: String,
  label: String?,
  selectable: Boolean,
  toggle: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  onLongClickLabel: String?,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val colors = MaterialTheme.colorScheme
  val inactiveContent = LocalContentColor.current
  // The vibrant toolbar sits on primaryContainer, so active items invert it for maximum contrast.
  val container by animateColorAsState(
    targetValue = if (active) colors.onPrimaryContainer else Color.Transparent,
    label = "toolbarContainer",
  )
  val content by animateColorAsState(
    targetValue = if (active) colors.primaryContainer else inactiveContent,
    label = "toolbarContent",
  )
  val corner by animateDpAsState(
    targetValue = if (pressed) 12.dp else 24.dp,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "toolbarCorner",
  )
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.9F else 1F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium),
    label = "toolbarScale",
  )
  Row(
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .animateContentSize(spring(dampingRatio = 0.6F, stiffness = Spring.StiffnessMediumLow))
      .height(48.dp)
      .widthIn(min = 48.dp)
      .clip(RoundedCornerShape(corner))
      .background(container)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(),
        role = if (toggle) Role.Switch else Role.Button,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
      )
      .semantics {
        this.contentDescription = contentDescription
        when {
          toggle -> toggleableState = ToggleableState(active)
          selectable -> selected = active
        }
      }
      .padding(horizontal = if (label != null) 10.dp else 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = content)
    if (label != null) {
      Spacer(Modifier.width(4.dp))
      RollingText(
        text = label,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = content,
      )
    }
  }
}
