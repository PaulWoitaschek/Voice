package de.ph1b.audiobook.uitools

import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.ImageView
import coil.bitmap.BitmapPool
import coil.size.Size
import coil.transform.Transformation
import de.ph1b.audiobook.features.imagepicker.CropOverlay

/**
 * Performs cropping based on the crop overlay
 */
class CropTransformation(cropOverlay: CropOverlay, private val cropSource: ImageView) :
  Transformation {

  private val rect = cropOverlay.selectedRect

  override fun key(): String = "cropTransformation"

  override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
    val scaleFactor: Float = input.width.toFloat() / cropSource.measuredWidth
    scaleRect(rect, scaleFactor)
    return try {
      Bitmap.createBitmap(input, rect.left, rect.top, rect.width(), rect.height())
    } finally {
      input.recycle()
    }
  }

  private fun scaleRect(rect: Rect, scaleFactor: Float) =
    rect.set(
      (rect.left * scaleFactor).toInt(),
      (rect.top * scaleFactor).toInt(),
      (rect.right * scaleFactor).toInt(),
      (rect.bottom * scaleFactor).toInt()
    )
}
