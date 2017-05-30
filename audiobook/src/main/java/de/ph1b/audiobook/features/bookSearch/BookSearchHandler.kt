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

  fun handle(search: BookSearch) {
    i { "handle $search" }
    when (search.mediaFocus) {
      MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> playArtist(search)
      MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE, "vnd.android.cursor.item/audio" -> {
        playAlbum(search)
      }
      MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> playAlbum(search)
      else -> playUnstructuredSearch(search.query)
    }
  }

  private fun playAlbum(search: BookSearch) {
    if (search.album != null) {
      val foundMatch = findAndPlayFirstMatch {
        val nameMatches = it.name.contains(search.album, ignoreCase = true)
        val artistMatches = search.artist == null || it.author?.contains(search.artist, ignoreCase = true) == true
        nameMatches && artistMatches
      }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  //Look for anything that might match the query
  private fun playUnstructuredSearch(query: String?) {
    if (query != null) {
      val foundMatch = findAndPlayFirstMatch {
        val bookNameMatches = it.name.contains(query, ignoreCase = true)
        val authorMatches = it.author?.contains(query, ignoreCase = true) == true
        val chapterNameMatches = it.chapters.any {
          it.name.contains(query, ignoreCase = true)
        }
        bookNameMatches || authorMatches || chapterNameMatches
      }
      if (foundMatch) return
    }

    //continue playback
    i { "continuing from search without query" }
    if (prefs.currentBookId.value == -1L) {
      repo.activeBooks.firstOrNull()?.id?.let { prefs.currentBookId.set(it) }
    }
    player.play()
  }

  private fun playArtist(search: BookSearch) {
    i { "playArtist" }
    if (search.artist != null) {
      val foundMatch = findAndPlayFirstMatch { it.author?.contains(search.artist, ignoreCase = true) == true }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  // Play the first book that matches to a selector. Returns if a book is being played
  private inline fun findAndPlayFirstMatch(selector: (Book) -> Boolean): Boolean {
    val book = repo.activeBooks.firstOrNull(selector)
    if (book != null) {
      i { "found a match ${book.name}" }
      prefs.currentBookId.value = book.id
      player.play()
      return true
    } else return false
  }
}
