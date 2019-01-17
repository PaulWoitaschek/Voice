package de.ph1b.audiobook

import androidx.collection.SparseArrayCompat
import de.ph1b.audiobook.common.sparseArray.emptySparseArray
import de.ph1b.audiobook.data.Chapter
import java.io.File
import java.util.UUID

internal object ChapterFactory {

  fun create(
    file: String = "First.mp3",
    parent: String = "/root/",
    duration: Int = 10000,
    lastModified: Long = 12345L,
    bookId: UUID,
    marks: SparseArrayCompat<String> = emptySparseArray()
  ) = Chapter(
    file = File(parent, file),
    name = file,
    duration = duration,
    fileLastModified = lastModified,
    marks = marks,
    bookId = bookId
  )
}
