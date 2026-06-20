package voice.core.ui.icons

/*
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/expand_more.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
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
public val VoiceIcons.ExpandMore: ImageVector
  get() =
    ImageVector.Builder(
      name = "ExpandMore",
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
          moveTo(12f, 15.38f)
          lineToRelative(-6f, -6f)
          lineTo(7.4f, 7.97f)
          lineToRelative(4.6f, 4.6f)
          lineToRelative(4.6f, -4.6f)
          lineTo(18f, 9.38f)
          lineToRelative(-6f, 6f)
          close()
        }
      }
      .build()
