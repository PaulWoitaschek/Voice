package de.ph1b.audiobook.features.bookSearch

import android.provider.MediaStore
import dagger.Reusable
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
    when (bookSearch.mediaFocus) {
      "vnd.android.cursor.item/*" -> {
        if (bookSearch.query?.isEmpty() == true) {
          // 'Any' search mode, get the last played book and play it
          val playedBooks = repo.activeBooks.filter {
            it.time != 0
          }
          if (playedBooks.isNotEmpty()) {
            prefs.currentBookId.value = playedBooks.first().id
            player.play()
          }
        } else {
          // 'Unstructured' search mode
          playUnstructuredSearch(bookSearch.query)
        }
      }
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
      val match = repo.activeBooks.firstOrNull {
        it.name.contains(album, ignoreCase = true) && (artist == null || it.author?.contains(artist, ignoreCase = true) == true)
      }
      i { "found a match ${match?.name}" }
      if (match != null) {
        prefs.currentBookId.value = match.id
        player.play()
      }
    }
  }

  /**
   * Look for anything that might match the query
   * @param query The search string to be used
   */
  private fun playUnstructuredSearch(query: String?) {
    if (query != null) {
      val match = repo.activeBooks.firstOrNull {
        it.name.contains(query, ignoreCase = true)
            || it.author?.contains(query, ignoreCase = true) == true
            || it.chapters.firstOrNull {
          it.name.contains(query, ignoreCase = true)
        } != null
      }
      i { "found a match ${match?.name}" }
      if (match != null) {
        prefs.currentBookId.value = match.id
        player.play()
      }
    } else {
      //continue playback
      i { "continuing from search without query" }
      player.play()
    }
  }

  private fun playArtist(query: String?) {
    if (query != null) {
      val match = repo.activeBooks.firstOrNull {
        it.author?.contains(query, ignoreCase = true) == true
      }
      i { "found a match ${match?.name}" }
      if (match != null) {
        prefs.currentBookId.value = match.id
        player.play()
      }
    } else {
      //continue playback
      i { "continuing from search without query" }
      player.play()
    }
  }
}