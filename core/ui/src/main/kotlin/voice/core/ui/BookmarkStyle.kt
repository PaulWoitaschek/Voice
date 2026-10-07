@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.Bookmark
import voice.core.ui.icons.VoiceIcons

/**
 * Each kind of bookmark has its own shape, color and icon, so the meaning doesn't depend on color
 * alone. Bookmarks the sleep timer set are a moon on a plain circle.
 */
@Immutable
data class BookmarkStyle(
  val polygon: RoundedPolygon,
  val container: Color,
  val content: Color,
  val pin: Color,
  val icon: ImageVector?,
)

@Composable
fun bookmarkStyle(
  kind: Bookmark.Kind,
  setBySleepTimer: Boolean,
): BookmarkStyle {
  val colors = MaterialTheme.colorScheme
  if (setBySleepTimer) {
    return BookmarkStyle(
      polygon = MaterialShapes.Circle,
      container = colors.surfaceContainerHighest,
      content = colors.onSurfaceVariant,
      pin = colors.outline,
      icon = VoiceIcons.Bedtime,
    )
  }
  return when (kind) {
    Bookmark.Kind.Note -> BookmarkStyle(
      polygon = MaterialShapes.Clover4Leaf,
      container = colors.primaryContainer,
      content = colors.onPrimaryContainer,
      pin = colors.primaryContainer,
      icon = VoiceIcons.Bookmark,
    )
    Bookmark.Kind.Favorite -> BookmarkStyle(
      polygon = MaterialShapes.Heart,
      container = colors.tertiaryContainer,
      content = colors.onTertiaryContainer,
      pin = colors.tertiary,
      icon = null,
    )
    Bookmark.Kind.Quote -> BookmarkStyle(
      polygon = MaterialShapes.Cookie6Sided,
      container = colors.secondaryContainer,
      content = colors.onSecondaryContainer,
      pin = colors.secondary,
      icon = VoiceIcons.FormatQuote,
    )
    Bookmark.Kind.Revisit -> BookmarkStyle(
      polygon = MaterialShapes.SoftBurst,
      container = colors.primary,
      content = colors.onPrimary,
      pin = colors.primary,
      icon = VoiceIcons.Replay,
    )
  }
}

/** The kind's shape with its icon on top. */
@Composable
fun BookmarkBadge(
  kind: Bookmark.Kind,
  setBySleepTimer: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  val style = bookmarkStyle(kind, setBySleepTimer)
  Box(
    modifier = modifier
      .size(size)
      .background(color = style.container, shape = style.polygon.toShape()),
    contentAlignment = Alignment.Center,
  ) {
    if (style.icon != null) {
      Icon(
        imageVector = style.icon,
        contentDescription = null,
        tint = style.content,
        modifier = Modifier.size(size * 0.45F),
      )
    }
  }
}
