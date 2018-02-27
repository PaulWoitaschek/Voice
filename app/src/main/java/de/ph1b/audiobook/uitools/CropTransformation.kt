package de.ph1b.audiobook.uitools

import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.ImageView
import com.squareup.picasso.Transformation
import de.ph1b.audiobook.features.imagepicker.CropOverlay

/**
 * Performs cropping based on the crop overlay
 */
class CropTransformation(cropOverlay: CropOverlay, private val cropSource: ImageView) :
  Transformation {

  private val rect = cropOverlay.selectedRect

  override fun key(): String = "cropTransformation"

  override fun transform(source: Bitmap): Bitmap {
    val scaleFactor: Float = source.width.toFloat() / cropSource.measuredWidth
    scaleRect(rect, scaleFactor)
    return try {
      Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
    } finally {
      source.recycle()
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
