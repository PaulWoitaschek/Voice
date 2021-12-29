package de.ph1b.audiobook.uitools

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

interface SimpleTarget : Target {

  override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
  }
  override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
  }
}
