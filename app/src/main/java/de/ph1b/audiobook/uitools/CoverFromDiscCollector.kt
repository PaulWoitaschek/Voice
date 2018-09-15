package de.ph1b.audiobook.uitools

import android.app.ActivityManager
import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.misc.FileRecognition
import de.ph1b.audiobook.misc.coverFile
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Class for retrieving covers from disc.
 */
class CoverFromDiscCollector(
  private val activityManager: ActivityManager,
  private val imageHelper: ImageHelper
) {

  private val picasso = Picasso.get()
  private val coverChangedSubject = PublishSubject.create<UUID>()

  /** Find and stores covers for each book */
  suspend fun findCovers(books: List<Book>) {
    books.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.coverFile()
    if (coverFile.exists())
      return

    val foundOnDisc = findAndSaveCoverFromDisc(book)
    if (foundOnDisc)
      return

    findAndSaveCoverEmbedded(book)
  }

  private suspend fun findAndSaveCoverEmbedded(book: Book) {
    getEmbeddedCover(book.content.chapters)?.let {
      val coverFile = book.coverFile()
      imageHelper.saveCover(it, coverFile)
      picasso.invalidate(coverFile)
      coverChangedSubject.onNext(book.id)
    }
  }

  private suspend fun findAndSaveCoverFromDisc(book: Book): Boolean {
    if (book.type === Book.Type.COLLECTION_FOLDER || book.type === Book.Type.SINGLE_FOLDER) {
      val root = File(book.root)
      if (root.exists()) {
        val images = root.walk().filter { FileRecognition.imageFilter.accept(it) }
        getCoverFromDisk(images.toList())?.let {
          val coverFile = book.coverFile()
          imageHelper.saveCover(it, coverFile)
          picasso.invalidate(coverFile)
          coverChangedSubject.onNext(book.id)
          return true
        }
      }
    }
    return false
  }

  /** emits the bookId of a cover that has changed */
  fun coverChanged(): Observable<UUID> = coverChangedSubject
    .hide()
    .observeOn(AndroidSchedulers.mainThread())

  /** Find the embedded cover of a chapter */
  private fun getEmbeddedCover(chapters: List<Chapter>): Bitmap? {
    chapters.forEachIndexed { index, (file) ->
      val cover = imageHelper.getEmbeddedCover(file)
      if (cover != null || index == 5) return cover
    }
    return null
  }

  /** Returns the first bitmap that could be parsed from an image file */
  private fun getCoverFromDisk(coverFiles: List<File>): Bitmap? {
    // if there are images, get the first one.
    val mi = ActivityManager.MemoryInfo()
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
}
