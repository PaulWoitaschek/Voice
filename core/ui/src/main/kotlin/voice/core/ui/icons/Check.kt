package voice.core.ui.icons

/*
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/check.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
 * Generated: 2026-06-20T10:59:46Z
 */
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
public val VoiceIcons.Check: ImageVector
  get() =
    ImageVector.Builder(
      name = "Check",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    )
      .apply {
        path(
          fill = SolidColor(Color.Black),
          fillAlpha = 1f,
          stroke = null,
          strokeAlpha = 1f,
          strokeLineWidth = 1f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Bevel,
          strokeLineMiter = 1f,
          pathFillType = PathFillType.Companion.NonZero,
        ) {
          moveTo(9.55f, 18f)
          lineTo(3.85f, 12.3f)
          lineTo(5.28f, 10.88f)
          lineToRelative(4.28f, 4.28f)
          lineTo(18.73f, 5.97f)
          lineTo(20.15f, 7.4f)
          lineTo(9.55f, 18f)
          close()
        }
      }
      .build()
