package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.Book
import java.io.File

/**
 * MVP for the book detail screen
 *
 * @author Paul Woitaschek
 */
interface BookPlayMvp {

  interface View {
    fun render(book: Book)
    fun finish()
    fun showPlaying(playing: Boolean)
    fun showLeftSleepTime(ms: Int)
    fun openSleepTimeDialog()
  }

  abstract class Presenter : de.ph1b.audiobook.mvp.Presenter<View>() {
    abstract fun playPause()
    abstract fun rewind()
    abstract fun fastForward()
    abstract fun next()
    abstract fun previous()
    abstract fun seekTo(position: Int, file: File? = null)
    abstract fun toggleSleepTimer()
  }
}