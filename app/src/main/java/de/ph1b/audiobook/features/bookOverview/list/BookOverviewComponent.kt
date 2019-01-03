package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.RoundRectOutlineProvider
import de.ph1b.audiobook.misc.dpToPx
import de.ph1b.audiobook.misc.formatTime
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_overview_row_list.*

class BookOverviewComponent(private val listener: BookClickListener, context: Context) :
  AdapterComponent<BookOverviewModel, BookOverviewHolder>(BookOverviewModel::class) {

  private val listConstraintSet = ConstraintSet().apply {
    clone(context, R.layout.book_overview_row_list)
  }

  private val gridConstraintSet = ConstraintSet().apply {
    clone(context, R.layout.book_overview_row_grid)
  }

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    return BookOverviewHolder(
      parent = parent,
      listener = listener,
      listConstraintSet = listConstraintSet,
      gridConstraintSet = gridConstraintSet
    )
  }

  override fun onBindViewHolder(model: BookOverviewModel, holder: BookOverviewHolder) {
    holder.bind(model)
  }
}

class BookOverviewHolder(
  parent: ViewGroup,
  private val listener: BookClickListener,
  private val listConstraintSet: ConstraintSet,
  private val gridConstraintSet: ConstraintSet
) :
  ExtensionsHolder(parent, R.layout.book_overview_row_list) {

  private var boundBook: Book? = null
  private val loadBookCover = LoadBookCover(this)
  private var isGridLayout = false

  init {
    val outlineProvider = RoundRectOutlineProvider(itemView.context.dpToPx(2F))
    itemView.clipToOutline = true
    itemView.outlineProvider = outlineProvider
    cover.clipToOutline = true
    cover.outlineProvider = outlineProvider
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
    author.text = model.author
    author.isVisible = model.author != null
    title.maxLines = if (model.author == null) 2 else 1

    cover.transitionName = model.transitionName
    remainingTime.text = formatTime(model.remainingTimeInMs.toLong())
    this.progress.progress = model.progress
    loadBookCover.load(model.book)

    if (isGridLayout != model.useGridView) {
      isGridLayout = model.useGridView
      val constraintSet = if (isGridLayout) gridConstraintSet else listConstraintSet
      constraintSet.applyTo(root)
    }

    playingIndicator.isVisible = model.isCurrentBook
  }
}
