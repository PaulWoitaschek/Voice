package voice.features.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

/** The play button of the player: a cookie while paused that turns into a rounded square while playing. */
@Composable
internal fun PlayButton(
  playing: Boolean,
  size: Dp,
  images: WidgetImages,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  ShapeButton(
    shape = if (playing) WidgetShape.Square else WidgetShape.Cookie9,
    size = size,
    icon = if (playing) R.drawable.widget_ic_pause else R.drawable.widget_ic_play,
    contentDescription = LocalContext.current.getString(
      if (playing) StringsR.string.playback_action_pause else StringsR.string.playback_action_play,
    ),
    images = images,
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
internal fun SkipButton(
  forward: Boolean,
  size: Dp,
  images: WidgetImages,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  ShapeButton(
    shape = WidgetShape.Circle,
    size = size,
    icon = if (forward) UiR.drawable.ic_fast_forward_white_36dp else UiR.drawable.ic_rewind_white_36dp,
    contentDescription = LocalContext.current.getString(
      if (forward) StringsR.string.playback_action_fast_forward else StringsR.string.playback_action_rewind,
    ),
    images = images,
    onClick = onClick,
    modifier = modifier,
    container = GlanceTheme.colors.secondaryContainer,
    content = GlanceTheme.colors.onSecondaryContainer,
    iconFraction = 0.5F,
  )
}

@Composable
internal fun ShapeButton(
  shape: WidgetShape,
  size: Dp,
  @DrawableRes icon: Int,
  contentDescription: String?,
  images: WidgetImages,
  onClick: Action?,
  modifier: GlanceModifier = GlanceModifier,
  container: ColorProvider = GlanceTheme.colors.primary,
  content: ColorProvider = GlanceTheme.colors.onPrimary,
  iconFraction: Float = 0.42F,
) {
  var boxModifier = modifier.size(size)
  if (onClick != null) {
    boxModifier = boxModifier.clickable(onClick)
  }
  if (contentDescription != null) {
    boxModifier = boxModifier.semantics { this.contentDescription = contentDescription }
  }
  Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
    Image(
      provider = ImageProvider(images.shape(shape, size)),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      colorFilter = ColorFilter.tint(container),
    )
    WidgetIcon(icon = icon, size = size * iconFraction, color = content)
  }
}

@Composable
internal fun ShapedCover(
  cover: WidgetCover,
  shape: WidgetShape,
  size: Dp,
  images: WidgetImages,
  modifier: GlanceModifier = GlanceModifier,
) {
  Image(
    provider = ImageProvider(images.shapedCover(cover, shape, size)),
    contentDescription = null,
    modifier = modifier.size(size),
    contentScale = ContentScale.FillBounds,
  )
}

/** A play or pause badge for the corner of a cover, with a ring that sets it off from the cover. */
@Composable
internal fun PlayBadge(
  playing: Boolean,
  size: Dp,
  images: WidgetImages,
  modifier: GlanceModifier = GlanceModifier,
) {
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Image(
      provider = ImageProvider(images.shape(WidgetShape.Circle, size)),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground),
    )
    ShapeButton(
      shape = if (playing) WidgetShape.Square else WidgetShape.Cookie9,
      size = size - 4.dp,
      icon = if (playing) R.drawable.widget_ic_pause else R.drawable.widget_ic_play,
      contentDescription = null,
      images = images,
      onClick = null,
      iconFraction = 0.5F,
    )
  }
}

@Composable
internal fun WidgetIcon(
  @DrawableRes icon: Int,
  size: Dp,
  color: ColorProvider,
  modifier: GlanceModifier = GlanceModifier,
) {
  Image(
    provider = ImageProvider(icon),
    contentDescription = null,
    modifier = modifier.size(size),
    colorFilter = ColorFilter.tint(color),
  )
}

/** The whole book's progress, wavy while playing, like the player's seek bar. */
@Composable
internal fun ProgressWave(
  progress: Float,
  wavy: Boolean,
  width: Dp,
  images: WidgetImages,
  modifier: GlanceModifier = GlanceModifier,
  active: ColorProvider = GlanceTheme.colors.primary,
  track: ColorProvider = GlanceTheme.colors.secondaryContainer,
) {
  val bitmaps = images.progress(progress, wavy, width)
  Box(modifier = modifier.width(width).height(PROGRESS_HEIGHT)) {
    Image(
      provider = ImageProvider(bitmaps.track),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
      colorFilter = ColorFilter.tint(track),
    )
    Image(
      provider = ImageProvider(bitmaps.active),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
      colorFilter = ColorFilter.tint(active),
    )
  }
}

@Composable
internal fun SpeedChip(
  speed: Float,
  modifier: GlanceModifier = GlanceModifier,
) {
  Box(
    modifier = modifier
      .pillBackground(GlanceTheme.colors.secondaryContainer)
      .padding(horizontal = 8.dp, vertical = 2.dp),
  ) {
    Text(
      text = formatSpeed(speed),
      style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium),
      maxLines = 1,
    )
  }
}

/** A filled button with an optional icon, like "Start over". */
@Composable
internal fun PillButton(
  text: String,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
  @DrawableRes icon: Int? = null,
) {
  Row(
    modifier = modifier
      .pillBackground(GlanceTheme.colors.primary)
      .clickable(onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      WidgetIcon(icon = icon, size = 18.dp, color = GlanceTheme.colors.onPrimary)
      Spacer(GlanceModifier.width(6.dp))
    }
    Text(
      text = text,
      style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
      maxLines = 1,
    )
  }
}

@Composable
internal fun TitleText(
  text: String,
  modifier: GlanceModifier = GlanceModifier,
  maxLines: Int = 1,
  fontSize: TextUnit = 16.sp,
  color: ColorProvider = GlanceTheme.colors.onSurface,
) {
  Text(
    text = text,
    modifier = modifier,
    style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Bold),
    maxLines = maxLines,
  )
}

@Composable
internal fun SubtitleText(
  text: String,
  modifier: GlanceModifier = GlanceModifier,
  maxLines: Int = 1,
  fontSize: TextUnit = 13.sp,
  color: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
) {
  Text(
    text = text,
    modifier = modifier,
    style = TextStyle(color = color, fontSize = fontSize),
    maxLines = maxLines,
  )
}

/** A line of text that leads with a small icon, like the moon before "Stops at 23:10". */
@Composable
internal fun IconSubtitle(
  @DrawableRes icon: Int,
  text: String,
  modifier: GlanceModifier = GlanceModifier,
  color: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    WidgetIcon(icon = icon, size = 14.dp, color = color)
    Spacer(GlanceModifier.width(4.dp))
    SubtitleText(text = text, color = color)
  }
}
