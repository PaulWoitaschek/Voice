package de.ph1b.audiobook.features.bookOverview.list

import android.view.ViewGroup
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.RoundRectOutlineProvider
import de.ph1b.audiobook.misc.dpToPx
import de.ph1b.audiobook.misc.formatTime
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_overview_row_list.*

class GridBookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<BookOverviewModel, BookOverviewHolder>(BookOverviewModel::class) {

  override val viewType = 42

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    return BookOverviewHolder(
      layoutRes = R.layout.book_overview_row_grid,
      parent = parent,
      listener = listener
    )
  }

  override fun onBindViewHolder(model: BookOverviewModel, holder: BookOverviewHolder) {
    holder.bind(model)
  }

  override fun isForViewType(model: Any): Boolean {
    return model is BookOverviewModel && model.useGridView
  }
}

class ListBookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<BookOverviewModel, BookOverviewHolder>(BookOverviewModel::class) {

  override val viewType = 43

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    return BookOverviewHolder(
      layoutRes = R.layout.book_overview_row_list,
      parent = parent,
      listener = listener
    )
  }

  override fun onBindViewHolder(model: BookOverviewModel, holder: BookOverviewHolder) {
    holder.bind(model)
  }

  override fun isForViewType(model: Any): Boolean {
    return model is BookOverviewModel && !model.useGridView
  }
}

class BookOverviewHolder(
  layoutRes: Int,
  parent: ViewGroup,
  private val listener: BookClickListener
) : ExtensionsHolder(parent, layoutRes) {

  private var boundBook: Book? = null
  private val loadBookCover = LoadBookCover(this)

  init {
    cover.clipToOutline = true
    cover.outlineProvider = RoundRectOutlineProvider(itemView.context.dpToPx(2F))
    itemView.setOnClickListener {
      boundBook?.let { book ->
        listener(book, BookOverviewClick.REGULAR)
      }
    }
    itemView.setOnLongClickListener {
      boundBook?.let { book ->
        listener(book, BookOverviewClick.MENU)
        true
      } ?: false
    }
  }

  fun bind(model: BookOverviewModel) {
    boundBook = model.book
    val name = model.name
    title.text = name
    if (model.useGridView) {
      title.maxLines = 2
    } else {
      author.text = model.author
      author.isVisible = model.author != null
      title.maxLines = if (model.author == null) 2 else 1
    }

    cover.transitionName = model.transitionName
    remainingTime.text = formatTime(model.remainingTimeInMs)
    this.progress.progress = model.progress
    loadBookCover.load(model.book)

    playingIndicator.isVisible = model.isCurrentBook
  }
}
