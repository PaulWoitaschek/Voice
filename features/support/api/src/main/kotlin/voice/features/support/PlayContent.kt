@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.supporter.SupporterBadge
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
    val subscription = content.subscription
    if (subscription != null) {
      PeriodToggle(
        period = subscription.period,
        onPeriodSelect = listener::selectPeriod,
      )
      Tiers(
        tiers = subscription.tiers,
        period = subscription.period,
        selectedTier = subscription.selectedTier,
        onTierSelect = listener::selectTier,
      )
    }
    if (message != null) {
      MessageCard(message)
    }
    if (subscription != null) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ActionButton(
          action = subscription.action,
          onClick = { activity?.let(listener::confirm) },
        )
        Text(
          text = stringResource(StringsR.string.support_fine_print),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        if (subscription.subscribed && subscription.action != Action.Manage) {
          TextButton(onClick = listener::manageSubscription) {
            Text(stringResource(StringsR.string.support_action_manage_subscription))
          }
        }
      }
    }
    if (content.tips.isNotEmpty()) {
      Tips(
        tips = content.tips,
        expanded = content.showTips,
        showToggle = subscription != null,
        onToggle = listener::toggleTips,
        onTip = { tip -> activity?.let { listener.tip(it, tip) } },
      )
    }
    BadgeGrowth(badge)
    if (content.note != null) {
      Note(content.note)
    }
  }
}

@Composable
private fun PeriodToggle(
  period: SupportPeriod,
  onPeriodSelect: (SupportPeriod) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    SupportPeriod.entries.forEachIndexed { index, entry ->
      ToggleButton(
        checked = period == entry,
        onCheckedChange = { onPeriodSelect(entry) },
        modifier = Modifier
          .weight(1F)
          .semantics { role = Role.RadioButton },
        shapes = if (index == 0) {
          ButtonGroupDefaults.connectedLeadingButtonShapes()
        } else {
          ButtonGroupDefaults.connectedTrailingButtonShapes()
        },
        colors = ToggleButtonDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurface,
          checkedContainerColor = MaterialTheme.colorScheme.secondary,
          checkedContentColor = MaterialTheme.colorScheme.onSecondary,
        ),
      ) {
        Text(
          stringResource(
            when (entry) {
              SupportPeriod.Monthly -> StringsR.string.support_period_monthly
              SupportPeriod.Yearly -> StringsR.string.support_period_yearly
            },
          ),
        )
      }
    }
  }
}

@Composable
private fun Tiers(
  tiers: List<SupportViewState.Tier>,
  period: SupportPeriod,
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
        period = period,
        selected = tier.tier == selectedTier,
        onClick = { onTierSelect(tier.tier) },
      )
    }
  }
}

@Composable
private fun TierCard(
  tier: SupportViewState.Tier,
  period: SupportPeriod,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val style = tier.tier.style()
  Surface(
    selected = selected,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .semantics { role = Role.RadioButton },
    shape = RoundedCornerShape(24.dp),
    color = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
    contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
    border = if (selected) BorderStroke(2.dp, colors.secondary) else null,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(style.container, style.shape.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(24.dp),
          imageVector = style.icon,
          contentDescription = null,
          tint = style.content,
        )
      }
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
      Text(
        modifier = Modifier.widthIn(max = 120.dp),
        text = pricePerPeriod(tier.formattedPrice, period),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.End,
      )
    }
  }
}

@Composable
private fun ActionButton(
  action: Action,
  onClick: () -> Unit,
) {
  val size = ButtonDefaults.MediumContainerHeight
  val text = when (action) {
    is Action.Subscribe -> stringResource(
      StringsR.string.support_action_subscribe,
      pricePerPeriod(action.formattedPrice, action.period),
    )
    is Action.Switch -> stringResource(
      StringsR.string.support_action_switch,
      pricePerPeriod(action.formattedPrice, action.period),
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
private fun Tips(
  tips: List<TipOffer>,
  expanded: Boolean,
  showToggle: Boolean,
  onToggle: () -> Unit,
  onTip: (TipOffer) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (showToggle) {
      TextButton(onClick = onToggle) {
        Text(stringResource(StringsR.string.support_action_tip_once))
      }
    }
    AnimatedVisibility(visible = expanded) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(StringsR.string.support_tips_title),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tips.forEach { tip ->
            FilledTonalButton(
              modifier = Modifier.weight(1F),
              onClick = { onTip(tip) },
              contentPadding = ButtonDefaults.SmallContentPadding,
            ) {
              Icon(
                modifier = Modifier.size(18.dp),
                imageVector = VoiceIcons.VolunteerActivism,
                contentDescription = null,
              )
              Spacer(Modifier.size(6.dp))
              Text(tip.formattedPrice, softWrap = false)
            }
          }
        }
      }
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
private fun pricePerPeriod(
  formattedPrice: String,
  period: SupportPeriod,
): String {
  return stringResource(
    when (period) {
      SupportPeriod.Monthly -> StringsR.string.support_price_monthly
      SupportPeriod.Yearly -> StringsR.string.support_price_yearly
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
      container = colors.primaryContainer,
      content = colors.onPrimaryContainer,
    )
    SupporterTier.GoldenMic -> TierStyle(
      shape = MaterialShapes.Sunny,
      icon = VoiceIcons.Mic,
      container = Color(0xFFF2C14E),
      content = Color(0xFF3D2E00),
    )
  }
}
