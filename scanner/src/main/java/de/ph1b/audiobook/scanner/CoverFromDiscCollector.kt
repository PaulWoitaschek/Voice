package de.ph1b.audiobook.scanner

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for retrieving covers from disc.
 */
@Singleton
class CoverFromDiscCollector
@Inject constructor(
  private val activityManager: ActivityManager,
  private val imageHelper: ImageHelper,
  private val context: Context
) {

  private val picasso = Picasso.get()
  private val coverChanged = BroadcastChannel<UUID>(1)

  /** Find and stores covers for each book */
  suspend fun findCovers(books: List<Book>) {
    books.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.coverFile(context)
    if (coverFile.exists())
      return

    val foundOnDisc = findAndSaveCoverFromDisc(book)
    if (foundOnDisc)
      return

    findAndSaveCoverEmbedded(book)
  }

  private suspend fun findAndSaveCoverEmbedded(book: Book) {
    getEmbeddedCover(book.content.chapters)?.let {
      val coverFile = book.coverFile(context)
      imageHelper.saveCover(it, coverFile)
      picasso.invalidate(coverFile)
      coverChanged.send(book.id)
    }
  }

  private suspend fun findAndSaveCoverFromDisc(book: Book): Boolean {
    if (book.type === Book.Type.COLLECTION_FOLDER || book.type === Book.Type.SINGLE_FOLDER) {
      val root = File(book.root)
      if (root.exists()) {
        val images = root.walk().filter { FileRecognition.imageFilter.accept(it) }
        getCoverFromDisk(images.toList())?.let {
          val coverFile = book.coverFile(context)
          imageHelper.saveCover(it, coverFile)
          picasso.invalidate(coverFile)
          coverChanged.send(book.id)
          return true
        }
      }
    }
    return false
  }

  /** emits the bookId of a cover that has changed */
  fun coverChanged(): Flow<UUID> = coverChanged.asFlow()

  /** Find the embedded cover of a chapter */
  private suspend fun getEmbeddedCover(chapters: List<Chapter>): Bitmap? {
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
