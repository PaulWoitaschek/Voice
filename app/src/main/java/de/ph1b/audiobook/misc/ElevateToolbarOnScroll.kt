package de.ph1b.audiobook.misc

import androidx.appcompat.widget.Toolbar
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView

class ElevateToolbarOnScroll(val toolbar: Toolbar) : RecyclerView.OnScrollListener() {

  private val interpolator = FastOutSlowInInterpolator()
  private val finalElevation = toolbar.context.dpToPxRounded(4F)

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    val toolbarHeight = toolbar.height
    if (toolbarHeight == 0) {
      return
    }
    val scrollY = recyclerView.computeVerticalScrollOffset()
    val fraction = (scrollY.toFloat() / toolbarHeight).coerceIn(0F, 1F)
    val interpolatedFraction = interpolator.getInterpolation(fraction)
    toolbar.elevation = interpolatedFraction * finalElevation
  }
}
