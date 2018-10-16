package de.ph1b.audiobook

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.Chapter
import java.util.UUID

object BookFactory {

  fun create(
    id: UUID = UUID.randomUUID(),
    type: Book.Type = Book.Type.SINGLE_FOLDER,
    author: String = "TestAuthor",
    currentFileIndex: Int = 0,
    time: Int = 0,
    name: String = "TestBook",
    playbackSpeed: Float = 1F,
    loudnessGain: Int = 500,
    skipSilence: Boolean = false,
    chapters: List<Chapter> = listOf(ChapterFactory.create(bookId = id))
  ): Book {

    val currentFile = chapters[currentFileIndex].file
    val root = if (currentFile.parent != null) currentFile.parent else "fakeRoot"

    return Book(
      id = id,
      metaData = BookMetaData(
        id = id,
        type = type,
        author = author,
        name = name,
        root = root,
        addedAtMillis = System.currentTimeMillis()
      ),
      content = BookContent(
        id = id,
        settings = BookSettings(
          id = id,
          currentFile = currentFile,
          positionInChapter = time,
          playbackSpeed = playbackSpeed,
          loudnessGain = loudnessGain,
          skipSilence = skipSilence,
          active = true,
          lastPlayedAtMillis = 0
        ),
        chapters = chapters
      )
    )
  }
}
