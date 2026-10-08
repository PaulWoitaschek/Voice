package voice.features.cover.crop

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.roundToInt

/** Cuts a square out of the image. [crop] is relative to the image, from 0 to 1 on both axes. */
internal class CropTransformation(private val crop: Rect) : Transformation {

  override val cacheKey: String = "crop(${crop.left},${crop.top},${crop.right},${crop.bottom})"

  override suspend fun transform(
    input: Bitmap,
    size: Size,
  ): Bitmap {
    val left = (crop.left * input.width).roundToInt().coerceIn(0, input.width - 1)
    val top = (crop.top * input.height).roundToInt().coerceIn(0, input.height - 1)
    val side = minOf(
      (crop.width * input.width).roundToInt(),
      (crop.height * input.height).roundToInt(),
      input.width - left,
      input.height - top,
    ).coerceAtLeast(1)
    return Bitmap.createBitmap(input, left, top, side, side)
  }
}
