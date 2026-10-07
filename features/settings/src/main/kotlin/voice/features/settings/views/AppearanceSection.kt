@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.ui.MorphShape
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberThemeColorScheme
import androidx.graphics.shapes.toPath as toAndroidPath
import voice.core.strings.R as StringsR

/**
 * Light or dark and the color scheme, picked by looking at them: little screens previewing each
 * mode and swatches for every palette, all in the colors they'd give the app.
 */
@Composable
internal fun AppearanceSection(
  themeMode: ThemeMode,
  themeColorScheme: ThemeColorScheme,
  dynamicColorAvailable: Boolean,
  onThemeModeSelect: (ThemeMode) -> Unit,
  onThemeColorSchemeSelect: (ThemeColorScheme) -> Unit,
  modifier: Modifier = Modifier,
) {
  // a stored dynamic scheme falls back to Voice blue where it's not supported, so that's what shows as selected
  val selectedColorScheme = if (themeColorScheme == ThemeColorScheme.Dynamic && !dynamicColorAvailable) {
    ThemeColorScheme.VoiceBlue
  } else {
    themeColorScheme
  }
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(Modifier.padding(vertical = 20.dp)) {
      Text(
        modifier = Modifier.padding(horizontal = 20.dp),
        text = stringResource(StringsR.string.settings_appearance_title),
        style = MaterialTheme.typography.titleLargeEmphasized,
      )
      Spacer(Modifier.height(16.dp))
      SectionLabel(stringResource(StringsR.string.settings_appearance_theme_title))
      Row(
        modifier = Modifier
          .selectableGroup()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ThemeMode.entries.forEach { mode ->
          ThemeModeTile(
            modifier = Modifier.weight(1F),
            themeMode = mode,
            themeColorScheme = selectedColorScheme,
            selected = mode == themeMode,
            onClick = { onThemeModeSelect(mode) },
          )
        }
      }
      Spacer(Modifier.height(24.dp))
      SectionLabel(stringResource(StringsR.string.settings_appearance_color_scheme_title))
      FlowRow(
        modifier = Modifier
          .selectableGroup()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ThemeColorScheme.entries
          .filter { it != ThemeColorScheme.Dynamic || dynamicColorAvailable }
          .forEach { scheme ->
            PaletteSwatch(
              themeColorScheme = scheme,
              selected = scheme == selectedColorScheme,
              onClick = { onThemeColorSchemeSelect(scheme) },
            )
          }
      }
      Spacer(Modifier.height(16.dp))
      SelectedColorScheme(selectedColorScheme)
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
}

/** A tiny screen in the colors of [themeMode]. Follow system shows both, split diagonally. */
@Composable
private fun ThemeModeTile(
  themeMode: ThemeMode,
  themeColorScheme: ThemeColorScheme,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val light = rememberThemeColorScheme(themeColorScheme, dark = false)
  val dark = rememberThemeColorScheme(themeColorScheme, dark = true)
  val playPath = remember { MaterialShapes.Cookie9Sided.toAndroidPath().asComposePath() }
  val playIconPath = remember { playIconPath() }
  val splitPath = remember { Path() }
  val scale by animateFloatAsState(
    targetValue = if (selected) 1F else 0.92F,
    animationSpec = spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessMediumLow),
    label = "tileScale",
  )
  val borderWidth by animateDpAsState(
    targetValue = if (selected) 3.dp else 1.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "tileBorderWidth",
  )
  val borderColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    label = "tileBorderColor",
  )
  val shape = RoundedCornerShape(20.dp)
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.75F)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    ) {
      Canvas(
        Modifier
          .fillMaxSize()
          .clip(shape)
          .border(borderWidth, borderColor, shape),
      ) {
        when (themeMode) {
          ThemeMode.Light -> drawMiniScreen(light, playPath, playIconPath)
          ThemeMode.Dark -> drawMiniScreen(dark, playPath, playIconPath)
          ThemeMode.FollowSystem -> {
            drawMiniScreen(light, playPath, playIconPath)
            splitPath.reset()
            splitPath.moveTo(size.width, 0F)
            splitPath.lineTo(size.width, size.height)
            splitPath.lineTo(0F, size.height)
            splitPath.close()
            clipPath(splitPath) {
              drawMiniScreen(dark, playPath, playIconPath)
            }
          }
        }
      }
      CheckBadge(
        visible = selected,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = themeMode.label(),
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

/** A cover, a title, a progress bar and a play button. Just enough to look like Voice. */
private fun DrawScope.drawMiniScreen(
  colors: ColorScheme,
  playPath: Path,
  playIconPath: Path,
) {
  drawRect(colors.surface)
  val coverSize = size.width * 0.5F
  val coverTop = size.height * 0.1F
  drawRoundRect(
    color = colors.primaryContainer,
    topLeft = Offset((size.width - coverSize) / 2, coverTop),
    size = Size(coverSize, coverSize),
    cornerRadius = CornerRadius(coverSize * 0.18F),
  )
  val lineHeight = size.height * 0.035F
  val titleTop = coverTop + coverSize + size.height * 0.05F
  drawRoundRect(
    color = colors.onSurface,
    topLeft = Offset(size.width * 0.24F, titleTop),
    size = Size(size.width * 0.52F, lineHeight),
    cornerRadius = CornerRadius(lineHeight / 2),
  )
  drawRoundRect(
    color = colors.onSurfaceVariant,
    alpha = 0.6F,
    topLeft = Offset(size.width * 0.32F, titleTop + lineHeight * 1.8F),
    size = Size(size.width * 0.36F, lineHeight),
    cornerRadius = CornerRadius(lineHeight / 2),
  )
  val progressTop = titleTop + lineHeight * 3.6F
  drawRoundRect(
    color = colors.secondaryContainer,
    topLeft = Offset(size.width * 0.14F, progressTop),
    size = Size(size.width * 0.72F, lineHeight),
    cornerRadius = CornerRadius(lineHeight / 2),
  )
  drawRoundRect(
    color = colors.primary,
    topLeft = Offset(size.width * 0.14F, progressTop),
    size = Size(size.width * 0.4F, lineHeight),
    cornerRadius = CornerRadius(lineHeight / 2),
  )
  val playSize = size.width * 0.24F
  val playTop = progressTop + lineHeight * 2.2F
  translate(left = (size.width - playSize) / 2, top = playTop) {
    scale(scaleX = playSize, scaleY = playSize, pivot = Offset.Zero) {
      drawPath(path = playPath, color = colors.primary)
      drawPath(path = playIconPath, color = colors.onPrimary)
    }
  }
}

/** A play triangle in a 1x1 box. */
private fun playIconPath(): Path = Path().apply {
  moveTo(0.4F, 0.32F)
  lineTo(0.7F, 0.5F)
  lineTo(0.4F, 0.68F)
  close()
}

/**
 * A swatch with the main colors of [themeColorScheme]. Selecting it puffs it up from a circle into
 * a cookie.
 */
@Composable
private fun PaletteSwatch(
  themeColorScheme: ThemeColorScheme,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5F
  val colors = rememberThemeColorScheme(themeColorScheme, dark)
  val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
  val progress by animateFloatAsState(
    targetValue = if (selected) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMediumLow),
    label = "swatchMorph",
  )
  val label = themeColorScheme.label()
  Box(
    modifier = Modifier
      .size(56.dp)
      .clip(CircleShape)
      .semantics { contentDescription = label }
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      Modifier
        .fillMaxSize()
        .graphicsLayer {
          rotationZ = progress * 40F
          // the cookie's bumps stick out, so it shrinks a little to keep the same visual size
          scaleX = 1F - progress * 0.04F
          scaleY = 1F - progress * 0.04F
        }
        .clip(MorphShape(morph, progress)),
    ) {
      drawRect(colors.primary, size = Size(size.width, size.height / 2))
      // the muted secondary and tertiary look grey at the tones of primary, lighter tones show their hue
      drawRect(colors.secondaryFixedDim, topLeft = Offset(0F, size.height / 2), size = Size(size.width / 2, size.height / 2))
      drawRect(colors.tertiaryFixedDim, topLeft = Offset(size.width / 2, size.height / 2), size = Size(size.width / 2, size.height / 2))
    }
    if (themeColorScheme == ThemeColorScheme.Dynamic && !selected) {
      Icon(
        imageVector = VoiceIcons.AutoAwesome,
        contentDescription = null,
        tint = colors.onPrimary,
        modifier = Modifier.size(20.dp),
      )
    }
    AnimatedVisibility(
      visible = selected,
      enter = scaleIn(spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium)) + fadeIn(),
      exit = scaleOut() + fadeOut(),
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(colors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = VoiceIcons.Check,
          contentDescription = null,
          tint = colors.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun CheckBadge(
  visible: Boolean,
  modifier: Modifier = Modifier,
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
        .background(MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

/** The name of the selected color scheme, rolling over to the next one when it changes. */
@Composable
private fun SelectedColorScheme(themeColorScheme: ThemeColorScheme) {
  AnimatedContent(
    modifier = Modifier.padding(horizontal = 20.dp),
    targetState = themeColorScheme,
    transitionSpec = {
      (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
    },
    label = "selectedColorScheme",
  ) { scheme ->
    Column {
      Text(
        text = scheme.label(),
        style = MaterialTheme.typography.titleMedium,
      )
      if (scheme == ThemeColorScheme.Dynamic) {
        Text(
          text = stringResource(StringsR.string.settings_appearance_color_scheme_dynamic_summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ThemeMode.label(): String {
  return when (this) {
    ThemeMode.FollowSystem -> stringResource(StringsR.string.settings_appearance_theme_follow_system)
    ThemeMode.Light -> stringResource(StringsR.string.settings_appearance_theme_light)
    ThemeMode.Dark -> stringResource(StringsR.string.settings_appearance_theme_dark)
  }
}

@Composable
private fun ThemeColorScheme.label(): String {
  return stringResource(
    when (this) {
      ThemeColorScheme.VoiceBlue -> StringsR.string.settings_appearance_color_scheme_voice_blue
      ThemeColorScheme.Dynamic -> StringsR.string.settings_appearance_color_scheme_dynamic
      ThemeColorScheme.Lagoon -> StringsR.string.settings_appearance_color_scheme_lagoon
      ThemeColorScheme.Forest -> StringsR.string.settings_appearance_color_scheme_forest
      ThemeColorScheme.Sunset -> StringsR.string.settings_appearance_color_scheme_sunset
      ThemeColorScheme.Berry -> StringsR.string.settings_appearance_color_scheme_berry
      ThemeColorScheme.Lavender -> StringsR.string.settings_appearance_color_scheme_lavender
    },
  )
}
