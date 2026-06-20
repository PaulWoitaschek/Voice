package voice.core.ui.icons

/*
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/arrow_forward.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
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
public val VoiceIcons.ArrowForward: ImageVector
  get() =
    ImageVector.Builder(
      name = "ArrowForward",
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
          moveTo(16.18f, 13f)
          horizontalLineTo(4f)
          verticalLineTo(11f)
          horizontalLineTo(16.18f)
          lineTo(10.58f, 5.4f)
          lineTo(12f, 4f)
          lineToRelative(8f, 8f)
          lineToRelative(-8f, 8f)
          lineTo(10.58f, 18.6f)
          lineTo(16.18f, 13f)
          close()
        }
      }
      .build()
