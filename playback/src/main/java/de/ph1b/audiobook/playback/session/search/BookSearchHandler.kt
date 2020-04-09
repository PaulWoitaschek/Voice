package de.ph1b.audiobook.playback.session.search

import android.provider.MediaStore
import dagger.Reusable
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.PlayerController
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * This class provides a single point of entry to find and play a book by a search query
 */
@Reusable
class BookSearchHandler
@Inject constructor(
  private val repo: BookRepository,
  private val player: PlayerController,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>
) {

  fun handle(search: BookSearch) {
    Timber.i("handle $search")
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
        val artistMatches =
          search.artist == null || it.author?.contains(search.artist, ignoreCase = true) == true
        nameMatches && artistMatches
      }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  // Look for anything that might match the query
  private fun playUnstructuredSearch(query: String?) {
    if (query != null) {
      val foundMatch = findAndPlayFirstMatch {
        val bookNameMatches = it.name.contains(query, ignoreCase = true)
        val authorMatches = it.author?.contains(query, ignoreCase = true) == true
        val chapterNameMatches = it.content.chapters.any { chapter ->
          chapter.name.contains(query, ignoreCase = true)
        }
        bookNameMatches || authorMatches || chapterNameMatches
      }
      if (foundMatch) return
    }

    // continue playback
    Timber.i("continuing from search without query")
    val currentId = currentBookIdPref.value
    val activeBooks = repo.activeBooks()
    val noBookInitialized = activeBooks.none { it.id == currentId }
    if (noBookInitialized) {
      activeBooks.firstOrNull()?.id?.let {
        currentBookIdPref.value = it
      }
    }
    player.play()
  }

  private fun playArtist(search: BookSearch) {
    Timber.i("playArtist")
    if (search.artist != null) {
      val foundMatch =
        findAndPlayFirstMatch { it.author?.contains(search.artist, ignoreCase = true) == true }
      if (foundMatch) return
    }

    playUnstructuredSearch(search.query)
  }

  // Play the first book that matches to a selector. Returns if a book is being played
  private inline fun findAndPlayFirstMatch(selector: (Book) -> Boolean): Boolean {
    val book = repo.activeBooks().firstOrNull(selector)
    return if (book != null) {
      Timber.i("found a match ${book.name}")
      currentBookIdPref.value = book.id
      player.play()
      true
    } else false
  }
}
