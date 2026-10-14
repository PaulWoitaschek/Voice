@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.CheckBadge
import voice.core.ui.ShapedIcon
import voice.core.ui.SupporterBadgeIcon
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.label
import voice.features.support.SupportViewState.Action
import voice.features.support.SupportViewState.Message
import voice.core.strings.R as StringsR

@Composable
internal fun PlayContent(
  content: SupportViewState.Content.Play,
  message: Message?,
  badge: SupporterBadge?,
  listener: SupportListener,
) {
  val activity = LocalActivity.current
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (content.frequencies.size > 1) {
      FrequencyToggle(
        frequencies = content.frequencies,
        frequency = content.frequency,
        onFrequencySelect = listener::selectFrequency,
      )
    }
    Tiers(
      tiers = content.tiers,
      frequency = content.frequency,
      selectedTier = content.selectedTier,
      onTierSelect = listener::selectTier,
    )
    if (message != null) {
      MessageCard(message)
    }
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ActionButton(
        action = content.action,
        frequency = content.frequency,
        onClick = { activity?.let(listener::confirm) },
      )
      Text(
        text = stringResource(
          if (content.action is Action.Tip) StringsR.string.support_fine_print_tip else StringsR.string.support_fine_print,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (content.action is Action.Switch) {
        TextButton(onClick = listener::manageSubscription) {
          Text(stringResource(StringsR.string.support_action_manage_subscription))
        }
      }
    }
    BadgeGrowth(badge)
    if (content.note != null) {
      Note(content.note)
    }
  }
}

@Composable
private fun FrequencyToggle(
  frequencies: List<SupportFrequency>,
  frequency: SupportFrequency,
  onFrequencySelect: (SupportFrequency) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    frequencies.forEachIndexed { index, entry ->
      ToggleButton(
        checked = frequency == entry,
        onCheckedChange = { onFrequencySelect(entry) },
        modifier = Modifier
          .weight(1F)
          .semantics { role = Role.RadioButton },
        shapes = when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          frequencies.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurface,
          checkedContainerColor = MaterialTheme.colorScheme.secondary,
          checkedContentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
      ) {
        val labelStyle = MaterialTheme.typography.labelLarge
        Text(
          text = stringResource(
            when (entry) {
              SupportFrequency.Monthly -> StringsR.string.support_period_monthly
              SupportFrequency.Yearly -> StringsR.string.support_period_yearly
              SupportFrequency.Once -> StringsR.string.support_period_once
            },
          ),
          style = labelStyle,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
          autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = labelStyle.fontSize),
        )
      }
    }
  }
}

@Composable
private fun Tiers(
  tiers: List<SupportViewState.Tier>,
  frequency: SupportFrequency,
  selectedTier: SupporterTier,
  onTierSelect: (SupporterTier) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .selectableGroup(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    tiers.forEach { tier ->
      TierCard(
        tier = tier,
        frequency = frequency,
        selected = tier.tier == selectedTier,
        onClick = { onTierSelect(tier.tier) },
      )
    }
  }
}

@Composable
private fun TierCard(
  tier: SupportViewState.Tier,
  frequency: SupportFrequency,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val containerColor by animateColorAsState(
    targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
    label = "tierContainer",
  )
  val contentColor by animateColorAsState(
    targetValue = if (selected) colors.onPrimaryContainer else colors.onSurface,
    label = "tierContent",
  )
  // with large fonts, a price beside the text leaves the title too little room
  val stacked = LocalDensity.current.fontScale >= 1.5F
  val priceDescription = pricePer(tier.formattedPrice, frequency)
  Surface(
    selected = selected,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .semantics { role = Role.RadioButton },
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
    contentColor = contentColor,
    border = if (selected) BorderStroke(2.dp, colors.primary) else null,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TierIcon(
        style = tier.tier.style(),
        selected = selected,
        ringColor = containerColor,
      )
      Column(Modifier.weight(1F)) {
        Text(
          text = tier.tier.title(),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = stringResource(
            when (tier.tier) {
              SupporterTier.Tea -> StringsR.string.support_tier_tea_summary
              SupporterTier.HoneyTea -> StringsR.string.support_tier_honey_tea_summary
              SupporterTier.GoldenMic -> StringsR.string.support_tier_golden_mic_summary
            },
          ),
          style = MaterialTheme.typography.bodyMedium,
        )
        if (stacked) {
          TierPrice(
            modifier = Modifier.padding(top = 4.dp),
            formattedPrice = tier.formattedPrice,
            description = priceDescription,
          )
        }
        if (tier.active) {
          Text(
            modifier = Modifier
              .padding(top = 4.dp)
              .background(colors.primary, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 2.dp),
            text = stringResource(StringsR.string.support_tier_current),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onPrimary,
          )
        }
      }
      if (!stacked) {
        TierPrice(
          modifier = Modifier.widthIn(max = 120.dp),
          formattedPrice = tier.formattedPrice,
          description = priceDescription,
        )
      }
    }
  }
}

@Composable
private fun TierPrice(
  formattedPrice: String,
  description: String,
  modifier: Modifier = Modifier,
) {
  Text(
    modifier = modifier.semantics { contentDescription = description },
    text = formattedPrice,
    style = MaterialTheme.typography.titleMedium,
    textAlign = TextAlign.End,
  )
}

@Composable
private fun TierIcon(
  style: TierStyle,
  selected: Boolean,
  ringColor: Color,
) {
  Box {
    ShapedIcon(
      icon = style.icon,
      shape = style.shape,
      containerColor = style.container,
      contentColor = style.content,
    )
    CheckBadge(
      visible = selected,
      outlineColor = ringColor,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(x = 6.dp, y = 6.dp),
    )
  }
}

@Composable
private fun ActionButton(
  action: Action,
  frequency: SupportFrequency,
  onClick: () -> Unit,
) {
  val size = ButtonDefaults.MediumContainerHeight
  val text = when (action) {
    is Action.Subscribe -> stringResource(
      StringsR.string.support_action_subscribe,
      pricePer(action.formattedPrice, frequency),
    )
    is Action.Switch -> stringResource(
      StringsR.string.support_action_switch,
      pricePer(action.formattedPrice, frequency),
    )
    is Action.Tip -> stringResource(
      StringsR.string.support_action_subscribe,
      pricePer(action.formattedPrice, frequency),
    )
    Action.Manage -> stringResource(StringsR.string.support_action_manage_subscription)
  }
  val content: @Composable () -> Unit = {
    Text(
      text = text,
      style = ButtonDefaults.textStyleFor(size),
      textAlign = TextAlign.Center,
    )
  }
  val modifier = Modifier
    .fillMaxWidth()
    .heightIn(size)
  if (action == Action.Manage) {
    FilledTonalButton(
      modifier = modifier,
      onClick = onClick,
      contentPadding = ButtonDefaults.contentPaddingFor(size),
      shapes = ButtonDefaults.shapesFor(size),
    ) { content() }
  } else {
    Button(
      modifier = modifier,
      onClick = onClick,
      contentPadding = ButtonDefaults.contentPaddingFor(size),
      shapes = ButtonDefaults.shapesFor(size),
    ) { content() }
  }
}

@Composable
private fun MessageCard(message: Message) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = when (message) {
      Message.Pending -> colors.tertiaryContainer
      Message.Failed -> colors.errorContainer
    },
    contentColor = when (message) {
      Message.Pending -> colors.onTertiaryContainer
      Message.Failed -> colors.onErrorContainer
    },
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = when (message) {
          Message.Pending -> VoiceIcons.HourglassEmpty
          Message.Failed -> VoiceIcons.Close
        },
        contentDescription = null,
      )
      Text(
        text = stringResource(
          when (message) {
            Message.Pending -> StringsR.string.support_message_pending
            Message.Failed -> StringsR.string.support_message_failed
          },
        ),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun BadgeGrowth(badge: SupporterBadge?) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(StringsR.string.support_badges_title),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SupporterBadge.growth.forEach { growth ->
        val earned = badge != null && badge.ordinal >= growth.ordinal
        val state = stringResource(
          if (earned) StringsR.string.support_badge_earned else StringsR.string.support_badge_not_earned,
        )
        Column(
          modifier = Modifier
            .weight(1F)
            .semantics(mergeDescendants = true) { stateDescription = state },
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SupporterBadgeIcon(
            badge = growth,
            size = 44.dp,
            earned = earned,
          )
          Text(
            text = growth.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun Note(note: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = VoiceIcons.FormatQuote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = note,
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(StringsR.string.support_note_signature),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
      )
    }
  }
}

@Composable
private fun pricePer(
  formattedPrice: String,
  frequency: SupportFrequency,
): String {
  return stringResource(
    when (frequency) {
      SupportFrequency.Monthly -> StringsR.string.support_price_monthly
      SupportFrequency.Yearly -> StringsR.string.support_price_yearly
      SupportFrequency.Once -> StringsR.string.support_price_once
    },
    formattedPrice,
  )
}

@Composable
private fun SupporterTier.title(): String {
  return stringResource(
    when (this) {
      SupporterTier.Tea -> StringsR.string.support_tier_tea_title
      SupporterTier.HoneyTea -> StringsR.string.support_tier_honey_tea_title
      SupporterTier.GoldenMic -> StringsR.string.support_tier_golden_mic_title
    },
  )
}

private class TierStyle(
  val shape: RoundedPolygon,
  val icon: ImageVector,
  val container: Color,
  val content: Color,
)

@Composable
private fun SupporterTier.style(): TierStyle {
  val colors = MaterialTheme.colorScheme
  return when (this) {
    SupporterTier.Tea -> TierStyle(
      shape = MaterialShapes.Circle,
      icon = VoiceIcons.EmojiFoodBeverage,
      container = colors.tertiaryContainer,
      content = colors.onTertiaryContainer,
    )
    SupporterTier.HoneyTea -> TierStyle(
      shape = MaterialShapes.Cookie6Sided,
      icon = VoiceIcons.Hive,
      container = colors.primary,
      content = colors.onPrimary,
    )
    SupporterTier.GoldenMic -> TierStyle(
      shape = MaterialShapes.Sunny,
      icon = VoiceIcons.Mic,
      container = Color(0xFFF2C14E),
      content = Color(0xFF3D2E00),
    )
  }
}
