@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

private class HelpTile(
  val title: String,
  val icon: ImageVector,
  val shape: RoundedPolygon,
  val containerColor: Color,
  val contentColor: Color,
  val onClick: () -> Unit,
)

@Composable
internal fun HelpSection(
  showAnalytics: Boolean,
  analyticsEnabled: Boolean,
  onFaqClick: () -> Unit,
  onGetHelpClick: () -> Unit,
  onReportClick: () -> Unit,
  onSuggestClick: () -> Unit,
  onAnalyticsToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val faq = HelpTile(
    title = stringResource(StringsR.string.settings_support_faq_title),
    icon = VoiceIcons.Help,
    shape = MaterialShapes.Cookie4Sided,
    containerColor = colors.primaryContainer,
    contentColor = colors.onPrimaryContainer,
    onClick = onFaqClick,
  )
  val getHelp = HelpTile(
    title = stringResource(StringsR.string.settings_support_get_support_title),
    icon = VoiceIcons.Forum,
    shape = MaterialShapes.Clover4Leaf,
    containerColor = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    onClick = onGetHelpClick,
  )
  val report = HelpTile(
    title = stringResource(StringsR.string.settings_support_report_issue_title),
    icon = VoiceIcons.BugReport,
    shape = MaterialShapes.Pentagon,
    containerColor = colors.secondaryContainer,
    contentColor = colors.onSecondaryContainer,
    onClick = onReportClick,
  )
  val suggest = HelpTile(
    title = stringResource(StringsR.string.settings_support_suggest_idea_title),
    icon = VoiceIcons.Lightbulb,
    shape = MaterialShapes.Sunny,
    containerColor = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    onClick = onSuggestClick,
  )
  SettingsIsland(
    modifier = modifier,
    title = stringResource(StringsR.string.settings_support_title),
    containerColor = colors.surfaceContainerHigh,
  ) {
    if (LocalDensity.current.fontScale >= 1.5F) {
      listOf(faq, getHelp, report, suggest).forEach { tile ->
        IslandRow(
          title = tile.title,
          onClick = tile.onClick,
          leading = { TileIcon(tile) },
        )
      }
    } else {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TileRow(faq, getHelp)
        TileRow(report, suggest)
      }
    }
    if (showAnalytics) {
      Spacer(Modifier.height(8.dp))
      IslandSwitchRow(
        title = stringResource(StringsR.string.settings_analytics_consent_title),
        checked = analyticsEnabled,
        onCheckedChange = { onAnalyticsToggle() },
        leading = {
          ShapedIcon(
            icon = VoiceIcons.Analytics,
            shape = MaterialShapes.Square,
            containerColor = colors.surfaceContainerHighest,
            contentColor = colors.onSurfaceVariant,
          )
        },
        summary = { Text(stringResource(StringsR.string.settings_analytics_consent_description)) },
      )
    }
  }
}

@Composable
private fun TileRow(
  start: HelpTile,
  end: HelpTile,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TileButton(
      tile = start,
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight(),
    )
    TileButton(
      tile = end,
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight(),
    )
  }
}

@Composable
private fun TileButton(
  tile: HelpTile,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val rotation by animateFloatAsState(
    targetValue = if (pressed) 45F else 0F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMediumLow),
    label = "tileIconRotation",
  )
  Surface(
    onClick = tile.onClick,
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceBright,
    interactionSource = interactionSource,
  ) {
    Column(Modifier.padding(16.dp)) {
      TileIcon(tile, rotation)
      Spacer(Modifier.height(12.dp))
      Text(
        text = tile.title,
        style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}

@Composable
private fun TileIcon(
  tile: HelpTile,
  rotation: Float = 0F,
) {
  ShapedIcon(
    icon = tile.icon,
    shape = tile.shape,
    containerColor = tile.containerColor,
    contentColor = tile.contentColor,
    size = 44.dp,
    shapeRotation = rotation,
  )
}
