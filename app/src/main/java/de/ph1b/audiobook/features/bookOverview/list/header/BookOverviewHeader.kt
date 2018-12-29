package de.ph1b.audiobook.features.bookOverview.list.header

import android.view.ViewGroup
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_overview_header.*

class BookOverviewHeaderHolder(parent: ViewGroup) : ExtensionsHolder(parent, R.layout.book_overview_header) {

  fun bind(model: BookOverviewHeaderModel) {
    val context = itemView.context
    text.text = when (model.category) {
      BookOverviewCategory.CURRENT -> context.getString(R.string.book_header_current)
      BookOverviewCategory.NOT_STARTED -> context.getString(R.string.book_header_not_started)
      BookOverviewCategory.FINISHED -> context.getString(R.string.book_header_completed)
    }
    showAll.isVisible = model.hasMore
  }
}

class BookOverviewHeaderComponent :
  AdapterComponent<BookOverviewHeaderModel, BookOverviewHeaderHolder>(BookOverviewHeaderModel::class) {

  override fun onCreateViewHolder(parent: ViewGroup) =
    BookOverviewHeaderHolder(parent)

  override fun onBindViewHolder(model: BookOverviewHeaderModel, holder: BookOverviewHeaderHolder) {
    holder.bind(model)
  }
}
