package de.ph1b.audiobook.scanner

import android.content.Context
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.toUri
import de.ph1b.audiobook.ffmpeg.ffmpeg
import javax.inject.Inject

class CoverScanner
@Inject constructor(
  private val context: Context,
  private val coverSaver: CoverSaver,
) {

  suspend fun scan(books: List<Book2>) {
    books.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book2) {
    val coverFile = book.content.cover
    if (coverFile != null && coverFile.exists())
      return

    val foundOnDisc = findAndSaveCoverFromDisc(book)
    if (foundOnDisc)
      return

    scanForEmbeddedCover(book)
  }

  private suspend fun findAndSaveCoverFromDisc(book: Book2): Boolean {
/*    withContext(Dispatchers.IO){
      val chapters = book.content.chapters todo

      val meop = book.id
    }
    if (book.type === Book.Type.COLLECTION_FOLDER || book.type === Book.Type.SINGLE_FOLDER) {
      val root = File(book.root)
      if (root.exists()) {
        val images = root.walk().filter { FileRecognition.imageFilter.accept(it) }
        getCoverFromDisk(images.toList())?.let {
          val coverFile = book.coverFile(context)
          imageHelper.saveCover(it, coverFile)
          picasso.invalidate(coverFile)
          coverChanged.emit(book.id)
          return true
        }
      }
    }*/
    return false
  }

  private suspend fun scanForEmbeddedCover(book: Book2) {
    val coverFile = coverSaver.newBookCoverFile()
    book.chapters
      .take(5).forEach { chapter ->
        ffmpeg(
          input = chapter.id.toUri(),
          context = context,
          command = listOf("-an", coverFile.absolutePath)
        )
        if (coverFile.exists() && coverFile.length() > 0) {
          coverSaver.setBookCover(coverFile, bookId = book.id)
          return
        }
      }
  }

/*
  */
  /** Returns the first bitmap that could be parsed from an image file *//*

  private fun getCoverFromDisk(coverFiles: List<File>): Bitmap? {
    // if there are images, get the first one.
    val mi = ActivityManager.MemoryInfo() todo
    activityManager.getMemoryInfo(mi)
    val dimen = imageHelper.smallerScreenSize
    // only read cover if its size is less than a third of the available memory
    coverFiles.filter { it.length() < (mi.availMem / 3L) }.forEach {
      try {
        return picasso.load(it)
          .resize(dimen, dimen)
          .onlyScaleDown()
          .centerCrop()
          .get()
      } catch (ex: IOException) {
        Timber.e(ex, "Error when saving cover $it")
      }
    }
    return null
  }
*/
}
