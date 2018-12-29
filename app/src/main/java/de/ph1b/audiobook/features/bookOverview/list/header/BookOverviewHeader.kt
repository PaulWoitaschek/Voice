package de.ph1b.audiobook.features.bookOverview.list.header

import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_overview_header.*

enum class BookOverviewHeaderType {
  CURRENT,
  NOT_STARTED,
  FINISHED
}

class BookOverviewHeaderHolder(parent: ViewGroup) : ExtensionsHolder(parent, R.layout.book_overview_header) {

  fun bind(type: BookOverviewHeaderType) {
    val context = itemView.context
    text.text = when (type) {
      BookOverviewHeaderType.CURRENT -> context.getString(R.string.book_header_current)
      BookOverviewHeaderType.NOT_STARTED -> context.getString(R.string.book_header_not_started)
      BookOverviewHeaderType.FINISHED -> context.getString(R.string.book_header_completed)
    }
  }
}

class BookOverviewHeaderComponent :
  AdapterComponent<BookOverviewHeaderType, BookOverviewHeaderHolder>(
    BookOverviewHeaderType::class
  ) {

  override fun onCreateViewHolder(parent: ViewGroup) =
    BookOverviewHeaderHolder(parent)

  override fun onBindViewHolder(model: BookOverviewHeaderType, holder: BookOverviewHeaderHolder) {
    holder.bind(model)
  }
}
