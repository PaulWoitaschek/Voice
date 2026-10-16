package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val VoiceTypography: Typography = Typography().run {
  copy(
    displayLarge = displayLarge.bookText(),
    displayMedium = displayMedium.bookText(),
    displaySmall = displaySmall.bookText(),
    headlineLarge = headlineLarge.bookText(),
    headlineMedium = headlineMedium.bookText(),
    headlineSmall = headlineSmall.bookText(),
    titleLarge = titleLarge.bookText(),
    displayLargeEmphasized = displayLargeEmphasized.bookText(),
    displayMediumEmphasized = displayMediumEmphasized.bookText(),
    displaySmallEmphasized = displaySmallEmphasized.bookText(),
    headlineLargeEmphasized = headlineLargeEmphasized.bookText(),
    headlineMediumEmphasized = headlineMediumEmphasized.bookText(),
    headlineSmallEmphasized = headlineSmallEmphasized.bookText(),
    titleLargeEmphasized = titleLargeEmphasized.bookText(),
  )
}

// For the words that come from the book, like its title, author, chapters and bookmarks.
fun TextStyle.bookText(): TextStyle = copy(
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
