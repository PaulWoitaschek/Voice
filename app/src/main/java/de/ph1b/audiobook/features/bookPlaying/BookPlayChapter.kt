package de.ph1b.audiobook.features.bookPlaying

import java.io.File

data class BookPlayChapter(
    val file: File,
    val start: Int,
    val stop: Int,
    val name: String) {

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
        number + " - " + chapterName
      }
    }
    return chapterName
  }
}
