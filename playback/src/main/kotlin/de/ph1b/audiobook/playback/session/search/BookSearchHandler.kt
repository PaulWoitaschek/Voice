package de.ph1b.audiobook.playback.session.search

import android.provider.MediaStore
import androidx.datastore.core.DataStore
import dagger.Reusable
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.PlayerController
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * This class provides a single point of entry to find and play a book by a search query
 */
@Reusable
class BookSearchHandler
@Inject constructor(
  private val repo: BookRepository,
  private val player: PlayerController,
  @CurrentBook
  private val currentBook: DataStore<Book.Id?>,
) {

  suspend fun handle(search: BookSearch) {
    Timber.i("handle $search")
    when (search.mediaFocus) {
      MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> playArtist(search)
      MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
      MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> playAlbum(search)
      else -> playUnstructuredSearch(search.query)
    }
  }

  private suspend fun playAlbum(search: BookSearch) {
    if (search.album != null) {
      val foundMatch = findAndPlayFirstMatch {
        val nameMatches = it.content.name.contains(search.album, ignoreCase = true)
        val artistMatches =
          search.artist == null || it.content.author?.contains(search.artist, ignoreCase = true) == true
        nameMatches && artistMatches
      }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  // Look for anything that might match the query
  private suspend fun playUnstructuredSearch(query: String?) {
    if (query != null) {
      val foundMatch = findAndPlayFirstMatch {
        val bookNameMatches = it.content.name.contains(query, ignoreCase = true)
        val authorMatches = it.content.author?.contains(query, ignoreCase = true) == true
        val chapterNameMatches = it.chapters.any { chapter ->
          chapter.name.contains(query, ignoreCase = true)
        }
        bookNameMatches || authorMatches || chapterNameMatches
      }
      if (foundMatch) return
    }

    // continue playback
    Timber.i("continuing from search without query")
    val currentId = currentBook.data.first()
    val activeBooks = repo.flow().first()
    val noBookInitialized = activeBooks.none { it.content.id == currentId }
    if (noBookInitialized) {

      activeBooks.firstOrNull()?.content?.id?.let { id ->
        currentBook.updateData { id }
      }
    }
    player.play()
  }

  private suspend fun playArtist(search: BookSearch) {
    Timber.i("playArtist")
    if (search.artist != null) {
      val foundMatch = findAndPlayFirstMatch {
        it.content.author?.contains(search.artist, ignoreCase = true) == true
      }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  // Play the first book that matches to a selector. Returns if a book is being played
  private suspend inline fun findAndPlayFirstMatch(selector: (Book) -> Boolean): Boolean {
    val book = repo.flow().first().firstOrNull(selector)
    return if (book != null) {
      Timber.i("found a match ${book.content.name}")
      currentBook.updateData { book.content.id }
      player.play()
      true
    } else false
  }
}
