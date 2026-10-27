package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.ChapterId
import voice.core.data.CustomMetadata
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BookRepositoryImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val contentRepo = BookContentRepoImpl(db.bookContentDao())
  private val customMetadataRepo = CustomMetadataRepoImpl(db.customMetadataDao())
  private val repo = BookRepositoryImpl(ChapterRepoImpl(db.chapterDao()), contentRepo, customMetadataRepo)

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `a name the files already have isn't kept, so the book follows its files`() = runTest {
    val content = content(name = "Dune", author = "Herbert")
    contentRepo.put(content)

    repo.updateCustomMetadata(content.id) { it.copy(name = "Dune", author = "Frank Herbert") }

    assertEquals(
      expected = CustomMetadata(content.id, author = "Frank Herbert"),
      actual = customMetadataRepo.get(content.id),
    )
  }

  @Test
  fun `a book that isn't in the library gets no edits`() = runTest {
    val id = BookId("dune")

    repo.updateCustomMetadata(id) { it.copy(name = "Dune Messiah") }

    assertEquals(expected = emptyList(), actual = db.customMetadataDao().all())
  }

  private fun content(
    name: String,
    author: String,
  ) = BookContent(
    id = BookId(name),
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = true,
    lastPlayedAt = Instant.EPOCH,
    addedAt = Instant.EPOCH,
    chapters = listOf(ChapterId("chapter")),
    currentChapter = ChapterId("chapter"),
    positionInChapter = 0,
    gain = 0F,
    metadata = BookMetadata(
      name = name,
      author = author,
      album = null,
      title = null,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      cover = null,
      fileLastModified = null,
    ),
  )
}
