@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons

internal val IslandShape = RoundedCornerShape(32.dp)

/**
 * A rounded island of related settings with a big [title], in one of the theme's container colors.
 * Content lines up with the title when it keeps [IslandContentPadding] to the island's edges.
 */
@Composable
internal fun SettingsIsland(
  title: String,
  containerColor: Color,
  modifier: Modifier = Modifier,
  contentColor: Color = contentColorFor(containerColor),
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = IslandShape,
    color = containerColor,
    contentColor = contentColor,
  ) {
    Column(Modifier.padding(vertical = 20.dp)) {
      IslandTitle(title)
      Spacer(Modifier.height(12.dp))
      content()
    }
  }
}

internal val IslandContentPadding = 24.dp

@Composable
internal fun IslandTitle(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    modifier = modifier
      .padding(horizontal = IslandContentPadding)
      .semantics { heading() },
    text = text,
    style = MaterialTheme.typography.titleLargeEmphasized,
  )
}

/** A small heading inside an island, e.g. above a group of tiles. */
@Composable
internal fun IslandLabel(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    modifier = modifier
      .padding(horizontal = IslandContentPadding)
      .semantics { heading() },
    text = text,
    style = MaterialTheme.typography.labelLargeEmphasized,
  )
}

/**
 * A row inside an island, leading with a shaped icon. Its ripple keeps a little distance to the
 * island's edges, while the text still lines up with the island's title.
 */
@Composable
internal fun IslandRow(
  title: String,
  onClick: () -> Unit,
  leading: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  summary: (@Composable () -> Unit)? = null,
  trailing: @Composable () -> Unit = { Chevron() },
) {
  IslandRowLayout(
    modifier = modifier
      .padding(horizontal = 12.dp)
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick),
    title = title,
    titleStyle = MaterialTheme.typography.titleMedium,
    leading = leading,
    summary = summary,
    trailing = trailing,
  )
}

/** An [IslandRow] that toggles a switch. The whole row is the switch for accessibility services. */
@Composable
internal fun IslandSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  leading: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  summary: (@Composable () -> Unit)? = null,
  titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
  IslandRowLayout(
    modifier = modifier
      .padding(horizontal = 12.dp)
      .clip(RoundedCornerShape(24.dp))
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    title = title,
    titleStyle = titleStyle,
    leading = leading,
    summary = summary,
    trailing = {
      Switch(
        checked = checked,
        onCheckedChange = null,
        thumbContent = {
          AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
          ) {
            Icon(
              modifier = Modifier.size(16.dp),
              imageVector = VoiceIcons.Check,
              contentDescription = null,
            )
          }
        },
      )
    },
  )
}

@Composable
private fun IslandRowLayout(
  title: String,
  titleStyle: TextStyle,
  leading: @Composable () -> Unit,
  summary: (@Composable () -> Unit)?,
  trailing: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 72.dp)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    leading()
    Column(
      modifier = Modifier.weight(1F),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = titleStyle,
      )
      if (summary != null) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
          CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = 0.8F),
            content = summary,
          )
        }
      }
    }
    trailing()
  }
}

/** Points to where a row leads, mirrored for right to left languages. */
@Composable
internal fun Chevron(modifier: Modifier = Modifier) {
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  Icon(
    modifier = modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
    imageVector = VoiceIcons.ChevronRight,
    contentDescription = null,
  )
}

/** A check that pops in on top of a selected tile. */
@Composable
internal fun CheckBadge(
  visible: Boolean,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primary,
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = scaleIn(spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium)) + fadeIn(),
    exit = scaleOut() + fadeOut(),
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(containerColor, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.Check,
        contentDescription = null,
        tint = contentColorFor(containerColor),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}
