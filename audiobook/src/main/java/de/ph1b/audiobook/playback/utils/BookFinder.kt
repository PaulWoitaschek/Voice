package de.ph1b.audiobook.playback.utils

import android.os.Bundle
import android.provider.MediaStore
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayerController
import i
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Matthias Kutscheid
 * This class provides a single point of entry to find a book by a search query from any point in the application
 */
@Singleton
class BookFinder
@Inject
constructor() {

  @Inject lateinit var repo: BookRepository
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var player: PlayerController

  /**
   * Find a book by a search query. THe extras may provide more details about what and how to search.
   */
  fun findBook(query: String?, extras: Bundle?) {
    val mediaFocus = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)

    // Some of these extras may not be available depending on the search mode
    val album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM)
    val artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST)
//    val genre = extras?.getString("android.intent.extra.genre")
    val playlist = extras?.getString("android.intent.extra.playlist")
//    val title = extras?.getString(MediaStore.EXTRA_MEDIA_TITLE)

    // Determine the search mode and use the corresponding extras
    when (mediaFocus) {
      null -> // 'Unstructured' search mode (backward compatible)
        playUnstructuredSearch(query)
      "vnd.android.cursor.item/*" -> if (query?.isEmpty() == true) {
        // 'Any' search mode, get the last played book and play it
        val playedBooks = repo.activeBooks.filter {
          it.time != 0
        }
        if(playedBooks.isNotEmpty()) {
          prefs.currentBookId.value = playedBooks.first().id
          player.play()
        }
      } else {
        // 'Unstructured' search mode
        playUnstructuredSearch(query)
      }
//      MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
//        // 'Genre' search mode
//            playGenre(genre);
//      }
      MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> // 'Artist' search mode
        playArtist(artist)
      MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE, // 'Album' search mode
      "vnd.android.cursor.item/audio" -> // 'Song' search mode
        playAlbum(album, artist)
      MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> // 'Playlist' search mode
        //playPlaylist(album, artist, genre, playlist, title);
        // use the playlist name or album to play a book as a playlist
        playAlbum(playlist?: album, artist)
    }
  }

  private fun playSong(album: String?, artist: String?, genre: String?, title: String?) {
    val match = repo.activeBooks.firstOrNull {
      (album == null || it.name.contains(album, ignoreCase = true)) &&
          (artist == null || it.author?.contains(artist, ignoreCase = true) == true) &&
          (title == null || it.chapters.firstOrNull {
            it.name.contains(title, ignoreCase = true)
          } != null)
    }
    i { "found a match ${match?.name}" }
    if (match != null) {
      prefs.currentBookId.value = match.id
      player.play()
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