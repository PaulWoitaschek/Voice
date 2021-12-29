package de.ph1b.audiobook.misc

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

class CircleOutlineProvider : ViewOutlineProvider() {

  override fun getOutline(view: View, outline: Outline) {
    outline.setOval(0, 0, view.width, view.height)
  }
}
