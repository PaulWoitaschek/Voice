package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderHolder
import de.ph1b.audiobook.misc.dpToPxRounded

class BookOverviewItemDecoration(context: Context) : ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    val childHolder = parent.getChildViewHolder(view)
    val bottom: Int
    val top: Int
    if (childHolder is BookOverviewHeaderHolder) {
      bottom = 0
      top = if (childHolder.adapterPosition == 0) {
        0
      } else {
        2 * margin
      }
    } else {
      bottom = margin
      top = 0
    }
    outRect.set(margin, top, margin, bottom)
  }
}
