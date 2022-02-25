package voice.app.features.bookPlaying.selectchapter

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.data.Book
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import voice.playback.PlayerController
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
    val book = runBlocking { bookRepository.get(bookId) }

    if (book == null) {
      Logger.d("no book found for $bookId. CloseScreen")
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
      val book = bookRepository.get(bookId) ?: return@launch
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
