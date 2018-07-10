package de.ph1b.audiobook.features.bookOverview.list

import androidx.core.view.doOnPreDraw
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.MAX_IMAGE_SIZE
import kotlinx.android.synthetic.main.book_shelf_row.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.io.File
import javax.inject.Inject

class LoadBookCover(holder: BookOverviewHolder) {

  @Inject
  lateinit var coverColorExtractor: CoverColorExtractor

  init {
    App.component.inject(this)
  }

  private val context = holder.itemView.context
  private val progress = holder.progress
  private val cover = holder.cover
  private val defaultProgressColor = context.color(R.color.primaryDark)

  private var boundFile: File? = null
  private var boundName: String? = null

  private var currentCoverBindingJob: Job? = null

  fun load(book: Book) {
    currentCoverBindingJob?.cancel()
    currentCoverBindingJob = launch(IO) {
      val coverFile = book.coverFile()
      val bookName = book.name

      if (boundName == book.name && boundFile?.length() == coverFile.length()) {
        return@launch
      }

      val coverReplacement = CoverReplacement(bookName, context)

      progress.color = defaultProgressColor
      val extractedColor = coverColorExtractor.extract(coverFile)
      progress.color = extractedColor ?: context.color(R.color.primaryDark)
      val shouldLoadImage = coverFile.canRead() && coverFile.length() < MAX_IMAGE_SIZE
      withContext(UI) {
        if (!isActive) return@withContext
        if (shouldLoadImage) {
          Picasso.get()
            .load(coverFile)
            .placeholder(coverReplacement)
            .into(cover)
        } else {
          Picasso.get()
            .cancelRequest(cover)
          // we have to set the replacement in onPreDraw, else the transition will fail.
          cover.doOnPreDraw { cover.setImageDrawable(coverReplacement) }
        }

        boundFile = coverFile
        boundName = bookName
      }
    }
  }
}
