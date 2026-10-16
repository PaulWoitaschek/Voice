package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

// Each mode sets more of the type scale in Literata than the one before.
enum class TypographyMode {
  Roboto,
  Headings,
  Titles,
  Full,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun voiceTypography(mode: TypographyMode): Typography = Typography().run {
  fun TextStyle.inLiterataFrom(from: TypographyMode): TextStyle = if (mode >= from) inLiterata() else this
  val headings = TypographyMode.Headings
  val titles = TypographyMode.Titles
  val full = TypographyMode.Full
  copy(
    displayLarge = displayLarge.inLiterataFrom(headings),
    displayMedium = displayMedium.inLiterataFrom(headings),
    displaySmall = displaySmall.inLiterataFrom(headings),
    headlineLarge = headlineLarge.inLiterataFrom(headings),
    headlineMedium = headlineMedium.inLiterataFrom(headings),
    headlineSmall = headlineSmall.inLiterataFrom(headings),
    titleLarge = titleLarge.inLiterataFrom(headings),
    titleMedium = titleMedium.inLiterataFrom(titles),
    titleSmall = titleSmall.inLiterataFrom(titles),
    bodyLarge = bodyLarge.inLiterataFrom(full),
    bodyMedium = bodyMedium.inLiterataFrom(full),
    bodySmall = bodySmall.inLiterataFrom(full),
    labelLarge = labelLarge.inLiterataFrom(full),
    labelMedium = labelMedium.inLiterataFrom(full),
    labelSmall = labelSmall.inLiterataFrom(full),
    displayLargeEmphasized = displayLargeEmphasized.inLiterataFrom(headings),
    displayMediumEmphasized = displayMediumEmphasized.inLiterataFrom(headings),
    displaySmallEmphasized = displaySmallEmphasized.inLiterataFrom(headings),
    headlineLargeEmphasized = headlineLargeEmphasized.inLiterataFrom(headings),
    headlineMediumEmphasized = headlineMediumEmphasized.inLiterataFrom(headings),
    headlineSmallEmphasized = headlineSmallEmphasized.inLiterataFrom(headings),
    titleLargeEmphasized = titleLargeEmphasized.inLiterataFrom(headings),
    titleMediumEmphasized = titleMediumEmphasized.inLiterataFrom(titles),
    titleSmallEmphasized = titleSmallEmphasized.inLiterataFrom(titles),
    bodyLargeEmphasized = bodyLargeEmphasized.inLiterataFrom(full),
    bodyMediumEmphasized = bodyMediumEmphasized.inLiterataFrom(full),
    bodySmallEmphasized = bodySmallEmphasized.inLiterataFrom(full),
    labelLargeEmphasized = labelLargeEmphasized.inLiterataFrom(full),
    labelMediumEmphasized = labelMediumEmphasized.inLiterataFrom(full),
    labelSmallEmphasized = labelSmallEmphasized.inLiterataFrom(full),
  )
}

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
