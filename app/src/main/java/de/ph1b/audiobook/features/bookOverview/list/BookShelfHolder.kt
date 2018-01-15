package de.ph1b.audiobook.features.bookOverview.list

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.onFirstPreDraw
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.maxImageSize
import de.ph1b.audiobook.uitools.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.book_shelf_row.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.File
import javax.inject.Inject

class BookShelfHolder(parent: ViewGroup, listener: (Book, BookShelfClick) -> Unit) : RecyclerView.ViewHolder(
    parent.layoutInflater().inflate(
        R.layout.book_shelf_row,
        parent,
        false
    )
), LayoutContainer {

  @Inject lateinit var coverColorExtractor: CoverColorExtractor

  override val containerView: View? get() = itemView
  private var boundBook: Book? = null
  private val coverSize = itemView.resources.getDimensionPixelSize(R.dimen.book_shelf_list_height)
  private val defaultProgressColor = itemView.context.color(R.color.primaryDark)

  init {
    App.component.inject(this)

    itemView.clipToOutline = true
    itemView.setOnClickListener {
      boundBook?.let { listener(it, BookShelfClick.REGULAR) }
    }
    edit.setOnClickListener {
      boundBook?.let { listener(it, BookShelfClick.MENU) }
    }
  }

  fun bind(book: Book) {
    boundBook = book
    val name = book.name
    title.text = name
    author.text = book.author
    author.visible = book.author != null
    title.maxLines = if (book.author == null) 2 else 1
    bindCover(book)

    cover.transitionName = book.coverTransitionName

    val globalPosition = book.position
    val totalDuration = book.duration
    val progress = globalPosition.toFloat() / totalDuration.toFloat()

    this.progress.progress = progress
  }

  private var colorExtractionJob: Job? = null


  private var boundFile: File? = null
  private var boundName: String? = null

  private fun bindCover(book: Book) {
    val coverFile = book.coverFile()
    val bookName = book.name

    if (boundName == book.name && boundFile?.length() == coverFile.length()) {
      return
    }
    boundFile = coverFile
    boundName = bookName

    val coverReplacement = CoverReplacement(bookName, itemView.context)

    progress.color = defaultProgressColor
    colorExtractionJob?.cancel()
    colorExtractionJob = launch(UI) {
      val extractedColor = coverColorExtractor.extract(coverFile)
      progress.color = extractedColor ?: itemView.context.color(R.color.primaryDark)
    }

    if (coverFile.canRead() && coverFile.length() < maxImageSize) {
      Picasso.with(itemView.context)
          .load(coverFile)
          .placeholder(coverReplacement)
          .into(cover)
    } else {
      Picasso.with(itemView.context)
          .cancelRequest(cover)
      // we have to set the replacement in onPreDraw, else the transition will fail.
      cover.onFirstPreDraw { cover.setImageDrawable(coverReplacement) }
    }
  }
}
