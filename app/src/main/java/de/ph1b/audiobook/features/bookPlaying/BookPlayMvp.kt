package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.data.Book
import java.io.File

/**
 * MVP for the book detail screen
 */
interface BookPlayMvp {

  interface View {
    fun render(book: Book)
    fun finish()
    fun showPlaying(playing: Boolean)
    fun showLeftSleepTime(ms: Int)
    fun openSleepTimeDialog()
    fun showBookmarkAdded()
  }

  abstract class Presenter : de.ph1b.audiobook.mvp.Presenter<View>() {
    abstract fun playPause()
    abstract fun rewind()
    abstract fun fastForward()
    abstract fun next()
    abstract fun previous()
    abstract fun seekTo(position: Int, file: File? = null)
    abstract fun toggleSleepTimer()
    abstract fun addBookmark()
    abstract fun toggleSkipSilence()
  }
}
