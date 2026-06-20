package voice.core.ui.icons

/*
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/download.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
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
val VoiceIcons.Download: ImageVector
  get() =
    ImageVector.Builder(
      name = "Download",
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
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(12f, 16f)
          lineTo(7f, 11f)
          lineTo(8.4f, 9.55f)
          lineToRelative(2.6f, 2.6f)
          verticalLineTo(4f)
          horizontalLineToRelative(2f)
          verticalLineToRelative(8.15f)
          lineToRelative(2.6f, -2.6f)
          lineTo(17f, 11f)
          lineToRelative(-5f, 5f)
          close()
          moveTo(6f, 20f)
          quadTo(5.18f, 20f, 4.59f, 19.41f)
          reflectiveQuadTo(4f, 18f)
          verticalLineTo(15f)
          horizontalLineTo(6f)
          verticalLineToRelative(3f)
          horizontalLineTo(18f)
          verticalLineTo(15f)
          horizontalLineToRelative(2f)
          verticalLineToRelative(3f)
          quadToRelative(0f, 0.82f, -0.59f, 1.41f)
          reflectiveQuadTo(18f, 20f)
          horizontalLineTo(6f)
          close()
        }
      }
      .build()
