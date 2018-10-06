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
import kotlinx.android.synthetic.main.book_overview_row.*

class BookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<Book, BookOverviewHolder>(Book::class) {

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    return BookOverviewHolder(parent, listener)
  }

  override fun onBindViewHolder(model: Book, holder: BookOverviewHolder) {
    holder.bind(model)
  }
}

class BookOverviewHolder(parent: ViewGroup, private val listener: BookClickListener) :
  ExtensionsHolder(parent, R.layout.book_overview_row) {

  private var boundBook: Book? = null
  private val loadBookCover = LoadBookCover(this)

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

  fun bind(book: Book) {
    boundBook = book
    val name = book.name
    title.text = name
    author.text = book.author
    author.isVisible = book.author != null
    title.maxLines = if (book.author == null) 2 else 1

    cover.transitionName = book.coverTransitionName

    val globalPosition = book.content.position
    val totalDuration = book.content.duration
    val progress = (globalPosition.toFloat() / totalDuration.toFloat())
      .coerceAtMost(1F)

    val remainingTimeMs = book.content.duration - book.content.position
    remainingTime.text = formatTime(remainingTimeMs.toLong())

    this.progress.progress = progress

    loadBookCover.load(book)
  }
}
