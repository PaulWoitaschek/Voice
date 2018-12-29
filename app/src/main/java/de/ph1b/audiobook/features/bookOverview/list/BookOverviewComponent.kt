package de.ph1b.audiobook.features.bookOverview.list

import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.RoundRectOutlineProvider
import de.ph1b.audiobook.misc.dpToPx
import de.ph1b.audiobook.misc.formatTime
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_overview_row.*
import timber.log.Timber

class BookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<BookOverviewModel, BookOverviewHolder>(BookOverviewModel::class) {

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    return BookOverviewHolder(parent, listener)
  }

  override fun onBindViewHolder(model: BookOverviewModel, holder: BookOverviewHolder) {
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

    playingIndicator.isVisible = model.isCurrentBook
  }

  fun setPlaying(playing: Boolean) {
    Timber.i("book=${boundBook?.name} changed to playing=$playing")
    playingIndicator.isVisible = playing
  }
}

data class BookOverviewModel(
  val name: String,
  val author: String?,
  val transitionName: String,
  @FloatRange(from = 0.0, to = 1.0)
  val progress: Float,
  val book: Book,
  val remainingTimeInMs: Int,
  val isCurrentBook: Boolean
) {

  constructor(book: Book, isCurrentBook: Boolean) : this(
    name = book.name,
    author = book.author,
    transitionName = book.coverTransitionName,
    book = book,
    progress = book.progress(),
    remainingTimeInMs = book.remainingTimeInMs(),
    isCurrentBook = isCurrentBook
  )

  fun areContentsTheSame(other: BookOverviewModel): Boolean {
    val oldBook = book
    val newBook = other.book
    return oldBook.id == newBook.id &&
        oldBook.content.position == newBook.content.position &&
        name == other.name &&
        isCurrentBook == other.isCurrentBook
  }

  fun areItemsTheSame(other: BookOverviewModel): Boolean {
    return book.id == other.book.id
  }
}

private fun Book.progress(): Float {
  val globalPosition = content.position
  val totalDuration = content.duration
  return (globalPosition.toFloat() / totalDuration.toFloat())
    .coerceAtMost(1F)
}

private fun Book.remainingTimeInMs(): Int {
  return content.duration - content.position
}
