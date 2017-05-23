package de.ph1b.audiobook.features.voiceSearch

import android.app.SearchManager
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.BookMocker
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.internals.BookStorage
import de.ph1b.audiobook.persistence.internals.InternalDb
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


/**
 * @author Matthias Kutscheid
 * A test case to easily test the voice search functionality for Android auto (and OK google commands)
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), application = App::class)
class SearchTest {

  private lateinit var repo: BookRepository
  private var dummyBookId: Long = -1

  @Before
  fun setUp() {
    val internalDb = InternalDb(RuntimeEnvironment.application)
    val moshi = Moshi.Builder().build()
    val internalBookRegister = BookStorage(internalDb, moshi)
    repo = BookRepository(internalBookRegister)
    val dummy = BookMocker.mock()
    repo.addBook(dummy)
    // the book ID is assigned during storage, so we have to get it here.
    dummyBookId = repo.activeBooks.first().id
  }

  @Test
  fun testUnstructuredSearchByBook() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    intent.putExtra(SearchManager.QUERY, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testUnstructuredSearchByArtist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    intent.putExtra(SearchManager.QUERY, "TestAuthor")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testUnstructuredSearchByChapter() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    intent.putExtra(SearchManager.QUERY, "first")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusAnyNonePlayed() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //anything, but no query means play anything (preferably the last book
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing has been played, so nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusArtist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //Play something from our test Author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //The dummy book should have been found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusArtistNoSuccess() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //try to play something from an unknown author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor1")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusAlbum() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //Play something from our test Author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //The dummy book should have been found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfArtist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //try to play something from an unknown author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor1")
    intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfAlbum() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //try to play something from an unknown author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "TestBook1")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //Play something from our test Author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    intent.putExtra(MediaStore.EXTRA_MEDIA_PLAYLIST, "TestBook")
    intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //The dummy book should have been found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @Test
  fun testMediaFocusPlaylistWithAlbumInsteadOfPlaylist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //Play something from our test Author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //The dummy book should have been found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(dummyBookId).withFailMessage("No book found for for given search query")
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfArtist() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //try to play something from an unknown author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor1")
    intent.putExtra(MediaStore.EXTRA_MEDIA_PLAYLIST, "TestBook")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfAlbum() {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
    //try to play something from an unknown author
    intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
    intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "TestAuthor")
    intent.putExtra(MediaStore.EXTRA_MEDIA_PLAYLIST, "TestBook1")
    val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().start().get()

    //nothing should be found
    Assertions.assertThat(activity.prefs.currentBookId.value).isEqualTo(-1).withFailMessage("No book found for for given search query")
  }
}
