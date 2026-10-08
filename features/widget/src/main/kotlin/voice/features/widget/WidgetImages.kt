package voice.features.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import voice.core.ui.R as UiR

internal enum class WidgetShape {
  Circle,
  Cookie9,
  Cookie12,
  Square,
  Clover,
  Arch,
  Sunny,
  Pentagon,
  SoftBurst,
}

internal data class ProgressImages(
  val active: Bitmap,
  val track: Bitmap,
)

/**
 * App widgets are RemoteViews, which can't clip to a path or draw a wave. So shaped covers, shaped
 * buttons and the progress line are drawn into bitmaps. They are white where they're not a cover, so
 * the widget tints them with its colors, which keeps them right in light and dark mode.
 */
@SingleIn(AppScope::class)
@Inject
class WidgetImages(private val context: Context) {

  private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
    override fun sizeOf(
      key: String,
      value: Bitmap,
    ): Int = value.allocationByteCount
  }

  internal suspend fun cover(cover: String?): WidgetCover {
    if (cover != null) {
      val key = "cover:$cover"
      val bitmap = cache.get(key) ?: load(cover)?.also { cache.put(key, it) }
      if (bitmap != null) {
        return WidgetCover(cover, bitmap)
      }
    }
    val placeholder = cache.get(PLACEHOLDER_KEY)
      ?: ContextCompat.getDrawable(context, UiR.drawable.album_art)!!
        .toBitmap(COVER_SIZE_PX, COVER_SIZE_PX)
        .also { cache.put(PLACEHOLDER_KEY, it) }
    return WidgetCover(PLACEHOLDER_KEY, placeholder)
  }

  private suspend fun load(cover: String): Bitmap? {
    val request = ImageRequest.Builder(context)
      .data(cover)
      .size(COVER_SIZE_PX)
      // RemoteViews can't hold hardware bitmaps, and the memory cache shared with the app may have one
      .allowHardware(false)
      .memoryCachePolicy(CachePolicy.DISABLED)
      .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    return result.drawable.toBitmap()
  }

  internal fun shapedCover(
    cover: WidgetCover,
    shape: WidgetShape,
    size: Dp,
  ): Bitmap {
    val px = size.toPx()
    return cached("shaped:${cover.key}:$shape:$px") {
      createBitmap(px, px).applyCanvas {
        drawPath(shape.path(px, px), coverPaint(cover.bitmap, px, px, alignTop = false))
      }
    }
  }

  internal fun coverArt(
    cover: WidgetCover,
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
  ): Bitmap {
    val scale = (MAX_COVER_ART_PX / max(width.toPxFloat(), height.toPxFloat())).coerceAtMost(1F)
    val widthPx = (width.toPxFloat() * scale).roundToInt().coerceAtLeast(1)
    val heightPx = (height.toPxFloat() * scale).roundToInt().coerceAtLeast(1)
    val radius = cornerRadius.toPxFloat() * scale
    return cached("art:${cover.key}:$widthPx:$heightPx:$radius") {
      createBitmap(widthPx, heightPx).applyCanvas {
        drawRoundRect(
          0F,
          0F,
          widthPx.toFloat(),
          heightPx.toFloat(),
          radius,
          radius,
          coverPaint(cover.bitmap, widthPx, heightPx, alignTop = true),
        )
      }
    }
  }

  internal fun shape(
    shape: WidgetShape,
    size: Dp,
  ): Bitmap {
    val px = size.toPx()
    return cached("shape:$shape:$px") {
      createBitmap(px, px).applyCanvas {
        drawPath(shape.path(px, px), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
      }
    }
  }

  /**
   * The progress line split in two, so the played part and the track can be tinted differently. The
   * played part is a wave while [wavy] and ends in a gap, and the track ends in a dot, like the
   * Material progress indicators.
   */
  internal fun progress(
    progress: Float,
    wavy: Boolean,
    width: Dp,
  ): ProgressImages {
    val widthPx = width.toPx()
    val heightPx = PROGRESS_HEIGHT.toPx()
    val fraction = (progress.coerceIn(0F, 1F) * 1000).roundToInt() / 1000F
    val stroke = PROGRESS_STROKE.toPxFloat()
    val half = stroke / 2
    val centerY = heightPx / 2F
    val activeEnd = half + (widthPx - stroke) * fraction
    val hasActive = activeEnd - half >= 1F
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = stroke
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
    val active = cached("progress:active:$widthPx:$fraction:$wavy") {
      createBitmap(widthPx, heightPx).applyCanvas {
        if (hasActive) {
          val amplitude = if (wavy) WAVE_AMPLITUDE.toPxFloat() else 0F
          drawPath(wavePath(half, activeEnd, centerY, amplitude, WAVE_LENGTH.toPxFloat()), linePaint)
        }
        drawCircle(widthPx - half, centerY, half, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
      }
    }
    val track = cached("progress:track:$widthPx:$fraction") {
      createBitmap(widthPx, heightPx).applyCanvas {
        val start = if (hasActive) activeEnd + stroke + PROGRESS_GAP.toPxFloat() else half
        val end = widthPx - half
        if (start < end) {
          drawLine(start, centerY, end, centerY, linePaint)
        }
      }
    }
    return ProgressImages(active = active, track = track)
  }

  private fun wavePath(
    start: Float,
    end: Float,
    centerY: Float,
    amplitude: Float,
    wavelength: Float,
  ): Path {
    val path = Path()
    path.moveTo(start, centerY)
    if (amplitude == 0F) {
      path.lineTo(end, centerY)
      return path
    }
    var x = start
    while (x < end) {
      x = (x + 1F).coerceAtMost(end)
      path.lineTo(x, centerY + amplitude * sin(2 * PI * (x - start) / wavelength).toFloat())
    }
    return path
  }

  private fun coverPaint(
    cover: Bitmap,
    width: Int,
    height: Int,
    alignTop: Boolean,
  ): Paint {
    val scale = max(width / cover.width.toFloat(), height / cover.height.toFloat())
    val dx = (width - cover.width * scale) / 2
    val dy = if (alignTop) 0F else (height - cover.height * scale) / 2
    val shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
      setLocalMatrix(
        Matrix().apply {
          setScale(scale, scale)
          postTranslate(dx, dy)
        },
      )
    }
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
  }

  private inline fun cached(
    key: String,
    create: () -> Bitmap,
  ): Bitmap {
    return cache.get(key) ?: create().also { cache.put(key, it) }
  }

  private fun Dp.toPxFloat(): Float = value * context.resources.displayMetrics.density

  private fun Dp.toPx(): Int = toPxFloat().roundToInt().coerceAtLeast(1)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun WidgetShape.polygon(): RoundedPolygon {
  return when (this) {
    WidgetShape.Circle -> MaterialShapes.Circle
    WidgetShape.Cookie9 -> MaterialShapes.Cookie9Sided
    WidgetShape.Cookie12 -> MaterialShapes.Cookie12Sided
    WidgetShape.Square -> MaterialShapes.Square
    WidgetShape.Clover -> MaterialShapes.Clover4Leaf
    WidgetShape.Arch -> MaterialShapes.Arch
    WidgetShape.Sunny -> MaterialShapes.Sunny
    WidgetShape.Pentagon -> MaterialShapes.Pentagon
    WidgetShape.SoftBurst -> MaterialShapes.SoftBurst
  }
}

private fun WidgetShape.path(
  width: Int,
  height: Int,
): Path {
  val path = polygon().toPath()
  path.transform(Matrix().apply { setScale(width.toFloat(), height.toFloat()) })
  return path
}

internal val PROGRESS_HEIGHT = 10.dp
private val PROGRESS_STROKE = 3.dp
private val PROGRESS_GAP = 4.dp
private val WAVE_AMPLITUDE = 2.2.dp
private val WAVE_LENGTH = 14.dp
private const val COVER_SIZE_PX = 512
private const val MAX_COVER_ART_PX = 720F
private const val CACHE_BYTES = 12 * 1024 * 1024
private const val PLACEHOLDER_KEY = "placeholder"
