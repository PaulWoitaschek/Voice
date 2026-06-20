package voice.core.ui.icons

/*
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/chevron_right.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
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
public val VoiceIcons.ChevronRight: ImageVector
  get() =
    ImageVector.Builder(
      name = "ChevronRight",
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
          moveTo(12.6f, 12f)
          lineTo(8f, 7.4f)
          lineTo(9.4f, 6f)
          lineToRelative(6f, 6f)
          lineToRelative(-6f, 6f)
          lineTo(8f, 16.6f)
          lineTo(12.6f, 12f)
          close()
        }
      }
      .build()
