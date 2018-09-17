package de.ph1b.audiobook.features.bookOverview.list

import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.dpToPx
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
    cover.clipToOutline = true
    cover.outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, itemView.context.dpToPx(2F))
      }
    }
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

    this.progress.progress = progress

    loadBookCover.load(book)
  }
}
