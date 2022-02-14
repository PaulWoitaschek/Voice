package de.ph1b.audiobook.features.bookPlaying.selectchapter

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.PlayerController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class SelectChapterViewModel
@Inject constructor(
  private val bookRepository: BookRepository,
  private val player: PlayerController
) {

  private val scope = MainScope()

  private val _viewEffects = MutableSharedFlow<SelectChapterViewEffect>(extraBufferCapacity = 1)
  val viewEffects: Flow<SelectChapterViewEffect> get() = _viewEffects

  lateinit var bookId: Book.Id

  fun viewState(): SelectChapterViewState {
    val book = runBlocking { bookRepository.flow(bookId).first() }

    if (book == null) {
      Timber.d("no book found for $bookId. CloseScreen")
      _viewEffects.tryEmit(SelectChapterViewEffect.CloseScreen)
      return SelectChapterViewState(emptyList(), null)
    }

    val chapterMarks = book.chapters.flatMap {
      it.chapterMarks
    }

    val selectedIndex = chapterMarks.indexOf(book.currentMark)
    return SelectChapterViewState(chapterMarks, selectedIndex.takeUnless { it == -1 })
  }

  fun chapterClicked(index: Int) {
    scope.launch {
      val book = bookRepository.flow(bookId).first() ?: return@launch
      var currentIndex = -1
      book.chapters.forEach { chapter ->
        chapter.chapterMarks.forEach { mark ->
          currentIndex++
          if (currentIndex == index) {
            player.setPosition(mark.startMs, chapter.id)
            _viewEffects.tryEmit(SelectChapterViewEffect.CloseScreen)
            return@launch
          }
        }
      }
    }
  }
}
