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
 *
 * @author Paul WOitaschek
 */
class CoverFromDiscCollector
@Inject constructor(context: Context, private val activityManager: ActivityManager, private val imageHelper: ImageHelper) {

  private val picasso = Picasso.with(context)

  /** Find and stores covers for each book */
  fun findCovers(books: List<Book>) {
    books.forEach {
      val coverFile = it.coverFile()
      if (!coverFile.exists()) {
        if (it.type === Book.Type.COLLECTION_FOLDER || it.type === Book.Type.SINGLE_FOLDER) {
          val root = File(it.root)
          if (root.exists()) {
            val images = root.walk().filter { FileRecognition.imageFilter.accept(it) }
            getCoverFromDisk(images.toList())?.let {
              imageHelper.saveCover(it, coverFile)
              picasso.invalidate(coverFile)
              return@forEach
            }
          }
        }
        getEmbeddedCover(it.chapters)?.let {
          imageHelper.saveCover(it, coverFile)
          picasso.invalidate(coverFile)
        }
      }
    }
  }


  /** Find the embedded cover of a chapter */
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

  /** Returns the first bitmap that could be parsed from an image file */
  @Throws(InterruptedException::class)
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
        e(ex) { "Error when saving cover $it" }
      }
    }
    return null
  }
}