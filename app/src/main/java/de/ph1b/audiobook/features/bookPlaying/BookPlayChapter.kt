package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import de.ph1b.audiobook.common.sparseArray.keyAtOrNull
import de.ph1b.audiobook.data.Chapter
import java.io.File

data class BookPlayChapter(
  val file: File,
  val start: Int,
  val stop: Int,
  val name: String
) {

  val duration = stop - start

  fun correctedName(index: Int): String {
    var chapterName = name

    // cutting leading zeros
    chapterName = chapterName.replaceFirst("^0".toRegex(), "")
    val number = (index + 1).toString()

    // desired format is "1 - Title"
    if (!chapterName.startsWith(number + " - ")) {
      // if getTitle does not match desired format
      chapterName = if (chapterName.startsWith(number)) {
        // if it starts with a number, a " - " should follow
        number + " - " + chapterName.substring(chapterName.indexOf(number) + number.length)
      } else {
        // if the name does not match at all, set the correct format
        "$number - $chapterName"
      }
    }
    return chapterName
  }
}

fun List<Chapter>.chaptersAsBookPlayChapters(): List<BookPlayChapter> {
  val data = ArrayList<BookPlayChapter>(size)
  forEach {
    if (it.marks.size() > 1) {
      it.marks.forEachIndexed { index, position, name ->
        val start = if (index == 0) 0 else position
        val nextPosition = it.marks.keyAtOrNull(index + 1)
        val stop = if (nextPosition == null) it.duration else nextPosition - 1
        data.add(BookPlayChapter(it.file, start, stop, name))
      }
    } else {
      data.add(BookPlayChapter(it.file, 0, it.duration, it.name))
    }
  }
  return data
}
