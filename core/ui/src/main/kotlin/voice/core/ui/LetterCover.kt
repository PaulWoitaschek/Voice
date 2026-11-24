package voice.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import com.materialkolor.hct.Hct

data class LetterCover(
  val letter: String,
  val background: Color,
  val content: Color,
)

fun letterCover(
  title: String,
  dark: Boolean,
): LetterCover? {
  val codePoint = title.codePoints().filter(Character::isLetterOrDigit).findFirst().orElse(-1)
  if (codePoint == -1) return null
  val hue = title.hashCode().mod(LETTER_COVER_HUES) * 360.0 / LETTER_COVER_HUES
  fun tone(tone: Double) = Color(Hct.from(hue, LETTER_COVER_CHROMA, tone).toInt())
  return LetterCover(
    letter = String(Character.toChars(Character.toUpperCase(codePoint))),
    background = tone(if (dark) 30.0 else 85.0),
    content = tone(if (dark) 90.0 else 25.0),
  )
}

private const val LETTER_COVER_HUES = 8
private const val LETTER_COVER_CHROMA = 36.0

const val LETTER_COVER_LETTER_SIZE = 0.6F

// centers the height of a capital, so the letter doesn't sit high over the room for descenders
fun letterBaseline(
  height: Float,
  fontSize: Float,
): Float = (height + fontSize * LITERATA_CAP_HEIGHT) / 2F

private const val LITERATA_CAP_HEIGHT = 0.7F

// Coil keeps the error painter of a failed request, so the painter stays and its title can change
@Composable
fun rememberLetterCover(title: String): Painter {
  val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5F
  val cover = letterCover(title, dark)
  val disc = painterResource(R.drawable.album_art)
  val textMeasurer = rememberTextMeasurer()
  val style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Medium)
  return remember(textMeasurer, style) { LetterCoverPainter(textMeasurer, style) }
    .apply {
      this.cover = cover
      this.disc = disc
    }
}

private class LetterCoverPainter(
  private val textMeasurer: TextMeasurer,
  private val style: TextStyle,
) : Painter() {

  var cover: LetterCover? by mutableStateOf(null)
  var disc: Painter? by mutableStateOf(null)

  override val intrinsicSize: Size = Size.Unspecified

  override fun DrawScope.onDraw() {
    val cover = cover
    if (cover == null) {
      disc?.run { draw(size) }
      return
    }
    drawRect(cover.background)
    val fontSize = size.minDimension * LETTER_COVER_LETTER_SIZE
    val text =
      textMeasurer.measure(
        text = cover.letter,
        style =
          style.copy(
            color = cover.content,
            fontSize = fontSize.toSp(),
            lineHeight = TextUnit.Unspecified,
          ),
      )
    drawText(
      textLayoutResult = text,
      topLeft =
        Offset(
          x = (size.width - text.size.width) / 2F,
          y = letterBaseline(size.height, fontSize) - text.firstBaseline,
        ),
    )
  }
}
