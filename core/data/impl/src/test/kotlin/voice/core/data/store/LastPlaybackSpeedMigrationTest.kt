package voice.core.data.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LastPlaybackSpeedMigrationTest {

  private val file = File(createTempDirectory().toFile(), "lastPlaybackSpeed")

  @Test
  fun `starts from the speed of the book played last`() = runTest {
    val migration = migration(
      content(id = "earlier", playbackSpeed = 1.25F, lastPlayedAt = Instant.ofEpochSecond(1)),
      content(id = "last", playbackSpeed = 1.75F, lastPlayedAt = Instant.ofEpochSecond(2)),
      content(id = "never played", playbackSpeed = 2F, lastPlayedAt = Instant.EPOCH),
    )

    assertTrue(migration.shouldMigrate(1F))
    assertEquals(expected = 1.75F, actual = migration.migrate(1F))
  }

  @Test
  fun `without a played book the speed stays`() = runTest {
    val migration = migration(content(id = "never played", playbackSpeed = 2F, lastPlayedAt = Instant.EPOCH))

    assertEquals(expected = 1F, actual = migration.migrate(1F))
  }

  @Test
  fun `a remembered speed is not migrated again`() = runTest {
    file.writeText("1.5")

    assertFalse(migration().shouldMigrate(1.5F))
  }

  private fun migration(vararg contents: BookContent) = LastPlaybackSpeedMigration(file, MemoryBookContentRepo(contents.toList()))
}

private fun content(
  id: String,
  playbackSpeed: Float,
  lastPlayedAt: Instant,
): BookContent {
  val chapter = ChapterId("$id-chapter")
  return BookContent(
    id = BookId(id),
    playbackSpeed = playbackSpeed,
    skipSilence = false,
    isActive = true,
    lastPlayedAt = lastPlayedAt,
    author = null,
    name = id,
    addedAt = Instant.EPOCH,
    chapters = listOf(chapter),
    currentChapter = chapter,
    positionInChapter = 0,
    cover = null,
    gain = 0F,
    genre = null,
    narrator = null,
    series = null,
    part = null,
  )
}

private class MemoryBookContentRepo(contents: List<BookContent>) : BookContentRepo {

  private val contents = MutableStateFlow(contents)

  override fun flow(): Flow<List<BookContent>> = contents

  override suspend fun all(): List<BookContent> = contents.value

  override fun flow(id: BookId): Flow<BookContent?> = contents.map { contents -> contents.find { it.id == id } }

  override suspend fun get(id: BookId): BookContent? = contents.value.find { it.id == id }

  override suspend fun setAllInactiveExcept(ids: List<BookId>) {
    contents.value = contents.value.map { it.copy(isActive = it.id in ids) }
  }

  override suspend fun put(content: BookContent) {
    contents.value = contents.value.filter { it.id != content.id } + content
  }
}
