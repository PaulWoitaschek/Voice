package voice.common

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

class CircleOutlineProvider : ViewOutlineProvider() {

  override fun getOutline(view: View, outline: Outline) {
    outline.setOval(0, 0, view.width, view.height)
  }
}
