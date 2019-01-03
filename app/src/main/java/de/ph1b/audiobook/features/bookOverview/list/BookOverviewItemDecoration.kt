package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderHolder
import de.ph1b.audiobook.misc.dpToPxRounded

class BookOverviewItemDecoration(
  context: Context,
  private val layoutManager: GridLayoutManager
) : ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)
  private val halfMargin = context.dpToPxRounded(4F)

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    val childHolder = parent.getChildViewHolder(view)
    val position = childHolder.adapterPosition
    val bottom: Int
    val top: Int
    if (childHolder is BookOverviewHeaderHolder) {
      bottom = 0
      top = if (position == 0) {
        0
      } else {
        2 * margin
      }
    } else {
      bottom = margin
      top = 0
    }

    val left: Int
    val right: Int
    val spanCount = layoutManager.spanCount
    val isGridLayout = spanCount > 1
    if (isGridLayout) {
      val spanIndex = layoutManager.spanSizeLookup.getSpanIndex(position, spanCount)
      val isLeft = spanIndex == 0
      val isRight = spanIndex == spanCount - 1
      left = if (isLeft) margin else halfMargin
      right = if (isRight) margin else halfMargin
    } else {
      left = margin
      right = margin
    }

    outRect.set(left, top, right, bottom)
  }
}
