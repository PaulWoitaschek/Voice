package voice.app.uitools

import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.ImageView
import coil.size.Size
import coil.transform.Transformation
import voice.app.features.imagepicker.CropOverlay

class CropTransformation(
  cropOverlay: CropOverlay,
  private val cropSource: ImageView,
) : Transformation {

  private val rect = cropOverlay.selectedRect

  override val cacheKey: String = "cropTransformation"

  override suspend fun transform(
    input: Bitmap,
    size: Size,
  ): Bitmap {
    val scaleFactor: Float = input.width.toFloat() / cropSource.measuredWidth
    scaleRect(rect, scaleFactor)
    return Bitmap.createBitmap(input, rect.left, rect.top, rect.width(), rect.height())
  }

  private fun scaleRect(
    rect: Rect,
    scaleFactor: Float,
  ) = rect.set(
    (rect.left * scaleFactor).toInt(),
    (rect.top * scaleFactor).toInt(),
    (rect.right * scaleFactor).toInt(),
    (rect.bottom * scaleFactor).toInt(),
  )
}
