package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ChapterRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()

  @After
  fun tearDown() {
    db.close()
  }

  private fun chapter(
    id: String,
    fileName: String?,
  ) = Chapter(
    id = ChapterId(id),
    name = id,
    duration = 1000,
    fileLastModified = Instant.EPOCH,
    fileSize = 10,
    markData = emptyList(),
    fileName = fileName,
  )

  @Test
  fun `a file whose provider reports no name keeps the name it had`() = runTest {
    val repo = ChapterRepoImpl(db.chapterDao())
    val stored = chapter("1", fileName = "1.mp3")
    repo.put(stored)

    val chapter = repo.getOrPut(stored.id, Instant.EPOCH, fileSize = 10, fileName = null) { error("analyzed again") }

    assertEquals(expected = stored, actual = chapter)
    assertEquals(expected = stored, actual = ChapterRepoImpl(db.chapterDao()).get(stored.id))
  }

  @Test
  fun `chapters are read in the order they are asked for`() = runTest {
    val repo = ChapterRepoImpl(db.chapterDao())
    val first = chapter("1", fileName = "1.mp3")
    val second = chapter("2", fileName = "2.mp3")
    db.chapterDao().insert(first)
    db.chapterDao().insert(second)

    val chapters = repo.get(listOf(second.id, ChapterId("missing"), first.id))

    assertEquals(expected = listOf(second, first), actual = chapters)
  }
}
