package voice.features.cover.crop

import android.graphics.Bitmap
import android.graphics.Rect
import coil.size.Size
import coil.transform.Transformation

class CropTransformation(
  cropOverlay: CropOverlay,
  private val sourceWidth: Int,
  private val sourceHeight: Int,
) : Transformation {

  private val rect = cropOverlay.selectedRect

  override val cacheKey: String = "cropTransformation"

  override suspend fun transform(
    input: Bitmap,
    size: Size,
  ): Bitmap {
    val scaleFactorX = input.width.toFloat() / sourceWidth
    val scaleFactorY = input.height.toFloat() / sourceHeight

    val scaled = Rect(
      (rect.left * scaleFactorX).toInt(),
      (rect.top * scaleFactorY).toInt(),
      (rect.right * scaleFactorX).toInt(),
      (rect.bottom * scaleFactorY).toInt(),
    )

    return Bitmap.createBitmap(
      input,
      scaled.left,
      scaled.top,
      scaled.width(),
      scaled.height(),
    )
  }
}
