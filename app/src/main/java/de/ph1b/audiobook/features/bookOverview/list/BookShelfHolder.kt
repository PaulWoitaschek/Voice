package de.ph1b.audiobook.features.bookOverview.list

import android.view.ViewGroup
import androidx.view.doOnPreDraw
import androidx.view.isVisible
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ExtensionsHolder
import de.ph1b.audiobook.uitools.MAX_IMAGE_SIZE
import kotlinx.android.synthetic.main.book_shelf_row.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.io.File
import javax.inject.Inject

class BookShelfHolder(parent: ViewGroup, listener: (Book, BookShelfClick) -> Unit) :
  ExtensionsHolder(
    parent.layoutInflater().inflate(
      R.layout.book_shelf_row,
      parent,
      false
    )
  ) {

  @Inject
  lateinit var coverColorExtractor: CoverColorExtractor

  private var boundBook: Book? = null
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
    author.isVisible = book.author != null
    title.maxLines = if (book.author == null) 2 else 1
    launch(UI) { bindCover(book) }

    cover.transitionName = book.coverTransitionName

    val globalPosition = book.position
    val totalDuration = book.duration
    val progress = globalPosition.toFloat() / totalDuration.toFloat()

    this.progress.progress = progress
  }

  private var boundFile: File? = null
  private var boundName: String? = null

  private suspend fun bindCover(book: Book) {
    withContext(IO) {
      val coverFile = book.coverFile()
      val bookName = book.name

      if (boundName == book.name && boundFile?.length() == coverFile.length()) {
        return@withContext
      }
      boundFile = coverFile
      boundName = bookName

      val coverReplacement = CoverReplacement(bookName, itemView.context)

      progress.color = defaultProgressColor
      val extractedColor = coverColorExtractor.extract(coverFile)
      progress.color = extractedColor ?: itemView.context.color(R.color.primaryDark)
      val shouldLoadImage = coverFile.canRead() && coverFile.length() < MAX_IMAGE_SIZE
      withContext(UI) {
        if (shouldLoadImage) {
          Picasso.with(itemView.context)
            .load(coverFile)
            .placeholder(coverReplacement)
            .into(cover)
        } else {
          Picasso.with(itemView.context)
            .cancelRequest(cover)
          // we have to set the replacement in onPreDraw, else the transition will fail.
          cover.doOnPreDraw { cover.setImageDrawable(coverReplacement) }
        }
      }
    }
  }
}
