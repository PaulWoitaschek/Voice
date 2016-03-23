package de.ph1b.audiobook

import de.ph1b.audiobook.misc.NaturalOrderComparator
import java.io.File

/**
 * Represents a chapter in a book.
 *
 * @author Paul Woitaschek
 */
data class Chapter(val file: File, val name: String, val duration: Int) : Comparable<Chapter> {

    init {
        check(name.isNotEmpty())
    }

    override fun compareTo(other: Chapter): Int {
        return NaturalOrderComparator.FILE_COMPARATOR.compare(file, other.file)
    }
}