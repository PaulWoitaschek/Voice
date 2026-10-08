@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.icons.VoiceIcons
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import voice.core.strings.R as StringsR

@Composable
fun SupporterBadgeIcon(
  badge: SupporterBadge,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  earned: Boolean = true,
  contentDescription: String? = null,
) {
  val clock = rememberAnimationClock(running = earned)
  val style = badge.style()
  val outline = MaterialTheme.colorScheme.outlineVariant
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    val shape = style.shape.toShape()
    Box(
      Modifier
        .matchParentSize()
        .graphicsLayer { rotationZ = clock.value * 12F }
        .then(
          if (earned) {
            Modifier.background(style.container, shape)
          } else {
            Modifier.border(2.dp, outline, shape)
          },
        ),
    )
    Icon(
      modifier = Modifier.size(size * 0.44F),
      imageVector = style.icon,
      contentDescription = contentDescription,
      tint = if (earned) style.content else outline,
    )
  }
}

@Composable
fun SupporterBadge.label(): String {
  return stringResource(
    when (this) {
      SupporterBadge.TipJar -> StringsR.string.support_badge_tip_jar
      SupporterBadge.FirstCup -> StringsR.string.support_badge_first_cup
      SupporterBadge.ThreeMonths -> StringsR.string.support_badge_three_months
      SupporterBadge.SixMonths -> StringsR.string.support_badge_six_months
      SupporterBadge.OneYear -> StringsR.string.support_badge_one_year
    },
  )
}

@Composable
fun supporterSinceText(since: YearMonth): String {
  val locale = LocalConfiguration.current.locales[0]
  val formatted = remember(since, locale) {
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")
    since.format(DateTimeFormatter.ofPattern(pattern, locale))
  }
  return stringResource(StringsR.string.support_supporter_since, formatted)
}

private class BadgeStyle(
  val shape: RoundedPolygon,
  val icon: ImageVector,
  val container: Color,
  val content: Color,
)

@Composable
private fun SupporterBadge.style(): BadgeStyle {
  val colors = MaterialTheme.colorScheme
  return when (this) {
    SupporterBadge.TipJar -> BadgeStyle(
      shape = MaterialShapes.Clover4Leaf,
      icon = VoiceIcons.VolunteerActivism,
      container = colors.tertiaryContainer,
      content = colors.onTertiaryContainer,
    )
    SupporterBadge.FirstCup -> BadgeStyle(
      shape = MaterialShapes.Cookie4Sided,
      icon = VoiceIcons.Favorite,
      container = colors.secondaryContainer,
      content = colors.onSecondaryContainer,
    )
    SupporterBadge.ThreeMonths -> BadgeStyle(
      shape = MaterialShapes.Cookie6Sided,
      icon = VoiceIcons.Favorite,
      container = colors.primaryContainer,
      content = colors.onPrimaryContainer,
    )
    SupporterBadge.SixMonths -> BadgeStyle(
      shape = MaterialShapes.Cookie9Sided,
      icon = VoiceIcons.Favorite,
      container = colors.primary,
      content = colors.onPrimary,
    )
    // gold reads as gold in every theme, so it doesn't follow the color scheme
    SupporterBadge.OneYear -> BadgeStyle(
      shape = MaterialShapes.VerySunny,
      icon = VoiceIcons.AutoAwesome,
      container = Color(0xFFF2C14E),
      content = Color(0xFF3D2E00),
    )
  }
}
