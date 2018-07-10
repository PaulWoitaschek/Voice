package de.ph1b.audiobook.features.bookOverview.list

import android.view.ViewGroup
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ExtensionsHolder
import kotlinx.android.synthetic.main.book_shelf_row.*

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
  ExtensionsHolder(parent, R.layout.book_shelf_row) {

  private var boundBook: Book? = null
  private val loadBookCover = LoadBookCover(this)

  init {
    itemView.clipToOutline = true
    itemView.setOnClickListener {
      boundBook?.let { listener(it, BookOverviewClick.REGULAR) }
    }
    edit.setOnClickListener {
      boundBook?.let { listener(it, BookOverviewClick.MENU) }
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

    this.progress.progress = progress

    loadBookCover.load(book)
  }
}
