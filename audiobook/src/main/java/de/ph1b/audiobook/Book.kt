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


import android.os.Environment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.NaturalOrderComparator
import i
import java.io.File
import java.util.*


/**
 * Represents a playable book.
 *
 * @param id the book id
 * @param author the author of the book. Might be null
 *
 * @author Paul Woitaschek
 */
data class Book(val id: Long,
                val type: Type,
                val useCoverReplacement: Boolean,
                val author: String?,
                val currentFile: File,
                val time: Int,
                val name: String,
                val chapters: List<Chapter>,
                val playbackSpeed: Float,
                val root: String) : Comparable<Book> {

    override fun compareTo(other: Book): Int {
        return NaturalOrderComparator.STRING_COMPARATOR.compare(name, other.name)
    }

    companion object {
        const val ID_UNKNOWN = -1L
        const val SPEED_MAX = 2F
        const val SPEED_MIN = 0.5F
    }

    private val COVER_TRANSITION_PREFIX = "bookCoverTransition_"


    init {
        val chapterFiles = ArrayList<File>(chapters.size)
        for ((file) in chapters) {
            chapterFiles.add(file)
        }
        check(playbackSpeed >= SPEED_MIN, { "speed $playbackSpeed must be >= $SPEED_MIN" })
        check(playbackSpeed <= SPEED_MAX) { "speed $playbackSpeed must be <= $SPEED_MAX" }
        check(chapters.isNotEmpty(), { "chapters must not be empty" })
        check(chapterFiles.contains(currentFile), { "$chapterFiles must contain current file $currentFile" })
        check(name.isNotEmpty(), { "name must not be empty" })
        check(root.isNotEmpty(), { "root must not be empty" })
    }

    /**
     * The transition name for the cover transition.
     */
    val coverTransitionName = COVER_TRANSITION_PREFIX + id


    /**
     * The global duration. It sums up the duration of all chapters.
     */
    val globalDuration: Int by lazy {
        var globalDuration = 0
        for ((file, name, duration) in chapters) {
            globalDuration += duration
        }
        globalDuration
    }


    /**
     * @return the global position. It sums up the duration of all elapsed chapters plus the position
     * * in the current chapter.
     */
    fun globalPosition(): Int {
        var globalPosition = 0
        for (c in chapters) {
            if (c == currentChapter()) {
                globalPosition += time
                return globalPosition
            } else {
                globalPosition += c.duration
            }
        }
        throw IllegalStateException("Current chapter was not found while looking up the global position")
    }

    fun currentChapter(): Chapter {
        for (c in chapters) {
            if (c.file.equals(currentFile)) {
                return c
            }
        }
        throw IllegalArgumentException("currentChapter has no valid id with" + " currentFile=" + currentFile)
    }

    fun nextChapter(): Chapter? {
        val currentIndex = chapters.indexOf(currentChapter())
        if (currentIndex < chapters.size - 1) {
            return chapters[currentIndex + 1]
        }
        return null
    }


    fun previousChapter(): Chapter? {
        val currentIndex = chapters.indexOf(currentChapter())
        if (currentIndex > 0) {
            return chapters[currentIndex - 1]
        }
        return null
    }


    fun coverFile(): File {
        val separator = File.separator

        val name = type.name + if (type == Type.COLLECTION_FILE || type == Type.COLLECTION_FOLDER) {
            // if its part of a collection, take the first file
            chapters.first().file.absolutePath.replace(separator, "")
        } else {
            // if its a single, just take the root
            root.replace(separator, "")
        } + ".jpg"

        val coverFile = File("${Environment.getExternalStorageDirectory().absolutePath}${separator}Android${separator}data$separator${App.component().context.packageName}",
                name)
        i { "CoverFile is $coverFile" }
        if (!coverFile.parentFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.parentFile.mkdirs()
        }
        return coverFile
    }

    enum class Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE
    }
}
