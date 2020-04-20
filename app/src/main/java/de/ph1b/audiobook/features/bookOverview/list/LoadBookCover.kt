package de.ph1b.audiobook.features.bookOverview.list

import androidx.core.view.doOnPreDraw
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.CoverReplacement
import de.ph1b.audiobook.common.MAX_IMAGE_SIZE
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.coverFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadBookCover(binding: BookOverviewBinding) {

  @Inject
  lateinit var coverColorExtractor: CoverColorExtractor

  init {
    appComponent.inject(this)
  }

  private val context = binding.root.context
  private val progress = binding.progress
  private val cover = binding.cover
  private val defaultProgressColor = context.getColor(R.color.progressColor)

  private var boundFileLength: Long = Long.MIN_VALUE
  private var boundName: String? = null

  private var currentCoverBindingJob: Job? = null

  fun load(book: Book) {
    currentCoverBindingJob?.cancel()
    currentCoverBindingJob = GlobalScope.launch(Dispatchers.IO) {
      val coverFile = book.coverFile()
      val bookName = book.name

      val coverFileLength = coverFile.length()
      if (boundName == book.name && boundFileLength == coverFileLength) {
        return@launch
      }

      withContext(Dispatchers.Main) {
        progress.color = defaultProgressColor
      }
      val extractedColor = coverColorExtractor.extract(coverFile)
      val shouldLoadImage = coverFileLength in 1 until MAX_IMAGE_SIZE
      withContext(Dispatchers.Main) {
        progress.color = extractedColor ?: defaultProgressColor
        val coverReplacement = CoverReplacement(bookName, context)
        if (!isActive) return@withContext
        if (shouldLoadImage) {
          Picasso.get()
            .load(coverFile)
            .placeholder(coverReplacement)
            .into(cover)
        } else {
          Picasso.get().cancelRequest(cover)
          // we have to set the replacement in onPreDraw, else the transition will fail.
          cover.doOnPreDraw { cover.setImageDrawable(coverReplacement) }
        }

        boundFileLength = coverFileLength
        boundName = bookName
      }
    }
  }
}
