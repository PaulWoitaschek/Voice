package de.ph1b.audiobook.misc

import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach

fun Toolbar.applyTheme() {
  menu.forEach {
    it.icon.setTint(Color.BLACK)
  }
}
