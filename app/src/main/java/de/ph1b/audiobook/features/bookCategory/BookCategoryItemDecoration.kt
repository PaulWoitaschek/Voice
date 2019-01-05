package de.ph1b.audiobook.features.bookCategory

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.ph1b.audiobook.misc.dpToPxRounded

class BookCategoryItemDecoration(context: Context, private val layoutManager: GridLayoutManager) :
  RecyclerView.ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)
  private val halfMargin = context.dpToPxRounded(4F)

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val position = parent.getChildViewHolder(view).adapterPosition
    val spanSizeLookup = layoutManager.spanSizeLookup
    val spanCount = layoutManager.spanCount

    val isTop = spanSizeLookup.getSpanGroupIndex(position, spanCount) == 0
    val top = if (isTop) {
      margin
    } else {
      0
    }

    val left: Int
    val right: Int
    val isGridLayout = spanCount > 1
    if (isGridLayout) {
      val spanIndex = spanSizeLookup.getSpanIndex(position, spanCount)
      val isLeft = spanIndex == 0
      val isRight = spanIndex == spanCount - 1
      left = if (isLeft) margin else halfMargin
      right = if (isRight) margin else halfMargin
    } else {
      left = margin
      right = margin
    }
    outRect.set(left, top, right, margin)
  }
}
