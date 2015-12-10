package de.ph1b.audiobook.model

import java.io.File

/**
 * Represents a bookmark in the book.
 *
 * @author Paul Woitaschek
 */
data class Bookmark(val mediaFile: File, val title: String, val time: Int) : Comparable<Bookmark> {

    init {
        check(title.isNotEmpty())
    }

    override fun compareTo(other: Bookmark): Int {
        // compare files
        val fileCompare = NaturalOrderComparator.FILE_COMPARATOR.compare(mediaFile, other.mediaFile)
        if (fileCompare != 0) {
            return fileCompare
        }

        // if files are the same compare time
        if (time > other.time) {
            return 1
        } else if (time < other.time) {
            return -1
        }

        // if time is the same compare the titles
        return NaturalOrderComparator.STRING_COMPARATOR.compare(title, other.title)
    }
}