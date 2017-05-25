package de.ph1b.audiobook.features.bookSearch

import android.provider.MediaStore
import dagger.Reusable
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import i
import javax.inject.Inject

/**
 * This class provides a single point of entry to find and play a book by a search query
 *
 * @author Matthias Kutscheid
 * @author Paul Woitaschek
 */
@Reusable class BookSearchHandler
@Inject constructor(
    private val repo: BookRepository,
    private val prefs: PrefsManager,
    private val player: PlayerController) {

  fun handle(bookSearch: BookSearch) {
    i { "handle $bookSearch" }
    when (bookSearch.mediaFocus) {
      MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> playArtist(bookSearch.artist)
      MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE, "vnd.android.cursor.item/audio" -> {
        playAlbum(bookSearch.album, bookSearch.artist)
      }
      MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> playAlbum(bookSearch.playList ?: bookSearch.album, bookSearch.artist)
      else -> playUnstructuredSearch(bookSearch.query)
    }
  }

  private fun playAlbum(album: String?, artist: String?) {
    if (album != null) {
      findAndPlayFirstMatch({
        val nameMatches = it.name.contains(album, ignoreCase = true)
        val artistMatches = artist == null || it.author?.contains(artist, ignoreCase = true) == true
        nameMatches && artistMatches
      })
    }
  }

  /**
   * Look for anything that might match the query
   * @param query The search string to be used
   */
  private fun playUnstructuredSearch(query: String?) {
    if (query != null) {
      findAndPlayFirstMatch({
        val bookNameMatches = it.name.contains(query, ignoreCase = true)
        val authorMatches = it.author?.contains(query, ignoreCase = true) == true
        val chapterNameMatches = it.chapters.any {
          it.name.contains(query, ignoreCase = true)
        }
        bookNameMatches || authorMatches || chapterNameMatches
      })

    } else {
      //continue playback
      i { "continuing from search without query" }
      if (prefs.currentBookId.value == -1L) {
        repo.activeBooks.firstOrNull()?.id?.let { prefs.currentBookId.set(it) }
      }
      player.play()
    }
  }

  private fun playArtist(query: String?) {
    i { "playArtist $query" }
    if (query != null) {
      findAndPlayFirstMatch(
          { it.author?.contains(query, ignoreCase = true) == true },
          { it.name.contains(query, ignoreCase = true) }
      )
    } else {
      //continue playback
      i { "continuing from search without query" }
      player.play()
    }
  }

  @Suppress("LoopToCallChain")
  private fun findAndPlayFirstMatch(vararg selectors: (Book) -> Boolean) {
    val books = repo.activeBooks
    for (s in selectors) {
      val book = books.firstOrNull { book -> s(book) }
      if (book != null) {
        i { "found a match ${book.name}" }
        prefs.currentBookId.value = book.id
        player.play()
        return
      }
    }
  }
}