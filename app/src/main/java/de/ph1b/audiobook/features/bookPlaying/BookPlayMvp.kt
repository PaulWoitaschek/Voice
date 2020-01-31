package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.data.Book
import java.io.File
import kotlin.time.Duration

/**
 * MVP for the book detail screen
 */
interface BookPlayMvp {

  interface View {
    fun render(book: Book)
    fun finish()
    fun showPlaying(playing: Boolean)
    fun showLeftSleepTime(duration: Duration)
    fun openSleepTimeDialog()
    fun showBookmarkAdded()
  }

  abstract class Presenter : de.ph1b.audiobook.mvp.Presenter<View>() {
    abstract fun playPause()
    abstract fun rewind()
    abstract fun fastForward()
    abstract fun seekTo(position: Long, file: File? = null)
    abstract fun toggleSleepTimer()
    abstract fun addBookmark()
    abstract fun toggleSkipSilence()
  }
}
