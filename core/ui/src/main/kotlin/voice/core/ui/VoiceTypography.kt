@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

enum class TypographyMode {
  Roboto,
  Headings,
  Content,
  Titles,
  Full,
}

internal val LocalTypographyMode = staticCompositionLocalOf { TypographyMode.Roboto }

// For the words that come from the book, like its title, author, chapters and bookmarks.
@Composable
@ReadOnlyComposable
fun TextStyle.bookText(): TextStyle = if (LocalTypographyMode.current == TypographyMode.Content) inLiterata() else this

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun voiceTypography(mode: TypographyMode): Typography = Typography().run {
  val headings = mode != TypographyMode.Roboto
  val titles = mode == TypographyMode.Titles || mode == TypographyMode.Full
  val full = mode == TypographyMode.Full
  copy(
    displayLarge = displayLarge.inLiterataIf(headings),
    displayMedium = displayMedium.inLiterataIf(headings),
    displaySmall = displaySmall.inLiterataIf(headings),
    headlineLarge = headlineLarge.inLiterataIf(headings),
    headlineMedium = headlineMedium.inLiterataIf(headings),
    headlineSmall = headlineSmall.inLiterataIf(headings),
    titleLarge = titleLarge.inLiterataIf(headings),
    titleMedium = titleMedium.inLiterataIf(titles),
    titleSmall = titleSmall.inLiterataIf(titles),
    bodyLarge = bodyLarge.inLiterataIf(full),
    bodyMedium = bodyMedium.inLiterataIf(full),
    bodySmall = bodySmall.inLiterataIf(full),
    labelLarge = labelLarge.inLiterataIf(full),
    labelMedium = labelMedium.inLiterataIf(full),
    labelSmall = labelSmall.inLiterataIf(full),
    displayLargeEmphasized = displayLargeEmphasized.inLiterataIf(headings),
    displayMediumEmphasized = displayMediumEmphasized.inLiterataIf(headings),
    displaySmallEmphasized = displaySmallEmphasized.inLiterataIf(headings),
    headlineLargeEmphasized = headlineLargeEmphasized.inLiterataIf(headings),
    headlineMediumEmphasized = headlineMediumEmphasized.inLiterataIf(headings),
    headlineSmallEmphasized = headlineSmallEmphasized.inLiterataIf(headings),
    titleLargeEmphasized = titleLargeEmphasized.inLiterataIf(headings),
    titleMediumEmphasized = titleMediumEmphasized.inLiterataIf(titles),
    titleSmallEmphasized = titleSmallEmphasized.inLiterataIf(titles),
    bodyLargeEmphasized = bodyLargeEmphasized.inLiterataIf(full),
    bodyMediumEmphasized = bodyMediumEmphasized.inLiterataIf(full),
    bodySmallEmphasized = bodySmallEmphasized.inLiterataIf(full),
    labelLargeEmphasized = labelLargeEmphasized.inLiterataIf(full),
    labelMediumEmphasized = labelMediumEmphasized.inLiterataIf(full),
    labelSmallEmphasized = labelSmallEmphasized.inLiterataIf(full),
  )
}

private fun TextStyle.inLiterataIf(condition: Boolean): TextStyle = if (condition) inLiterata() else this

private fun TextStyle.inLiterata(): TextStyle = copy(
  fontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { weight ->
      Font(
        resId = R.font.literata,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
          FontVariation.weight(weight),
          FontVariation.opticalSizing(fontSize),
        ),
      )
    },
  ),
)
