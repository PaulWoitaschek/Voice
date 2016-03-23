/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook

import de.ph1b.audiobook.misc.NaturalOrderComparator
import java.io.File

/**
 * Represents a bookmark in the book.
 *
 * @author Paul Woitaschek
 */
data class Bookmark(val mediaFile: File, val title: String, val time: Int, val id: Long = ID_UNKNOWN) : Comparable<Bookmark> {

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

    companion object {
        val ID_UNKNOWN = -1L
    }
}