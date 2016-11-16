package de.ph1b.audiobook.uitools

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.misc.FileRecognition
import e
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Class for retrieving covers from disc.
 */
class CoverFromDiscCollector
@Inject constructor(context: Context, private val activityManager: ActivityManager, private val imageHelper: ImageHelper) {

  private val picasso = Picasso.with(context)

  /**
   * Trys to find covers and saves them to storage if found.

   * @throws InterruptedException
   */
  fun findCovers(books: List<Book>) {
    for (b in books) {
      val coverFile = b.coverFile()
      if (!coverFile.exists()) {
        if (b.type === Book.Type.COLLECTION_FOLDER || b.type === Book.Type.SINGLE_FOLDER) {
          val root = File(b.root)
          if (root.exists()) {
            val images = root.walk().filter { FileRecognition.imageFilter.accept(it) }
            val cover = getCoverFromDisk(images.toList())
            if (cover != null) {
              imageHelper.saveCover(cover, coverFile)
              picasso.invalidate(coverFile)
              continue
            }
          }
        }
        val cover = getEmbeddedCover(b.chapters)
        if (cover != null) {
          imageHelper.saveCover(cover, coverFile)
          picasso.invalidate(coverFile)
        }
      }
    }
  }


  /**
   * Finds an embedded cover within a [Chapter]

   * @param chapters The chapters to search trough
   * *
   * @return An embedded cover if there is one. Else return `null`
   * *
   * @throws InterruptedException If the scanner has been requested to reset.
   */
  @Throws(InterruptedException::class)
  private fun getEmbeddedCover(chapters: List<Chapter>): Bitmap? {
    var tries = 0
    val maxTries = 5
    for ((file) in chapters) {
      if (++tries < maxTries) {
        val cover = imageHelper.getEmbeddedCover(file)
        if (cover != null) {
          return cover
        }
      } else {
        return null
      }
    }
    return null
  }


  /**
   * Returns a Bitmap from an array of [File] that should be images

   * @param coverFiles The image files to check
   * *
   * @return A bitmap or `null` if there is none.
   * *
   * @throws InterruptedException If the scanner has been requested to reset.
   */
  @Throws(InterruptedException::class)
  private fun getCoverFromDisk(coverFiles: List<File>): Bitmap? {
    // if there are images, get the first one.
    val mi = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(mi)
    val dimen = imageHelper.smallerScreenSize
    // only read cover if its size is less than a third of the available memory
    coverFiles.filter { it.length() < (mi.availMem / 3L) }.forEach {
      try {
        return picasso.load(it).resize(dimen, dimen).get()
      } catch (ex: IOException) {
        e(ex) { "Error when saving cover $it" }
      }
    }
    return null
  }


}