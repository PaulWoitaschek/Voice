package de.ph1b.audiobook.features.bookCategory

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.ph1b.audiobook.misc.dpToPxRounded

class BookOverviewItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val position = parent.getChildViewHolder(view).adapterPosition
    val top = if (position == 0) {
      margin
    } else {
      0
    }
    outRect.set(margin, top, margin, margin)
  }
}
