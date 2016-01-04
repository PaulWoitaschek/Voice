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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.model


import android.os.Environment
import com.google.common.collect.ImmutableList
import de.ph1b.audiobook.injection.App
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
                val bookmarks: ImmutableList<Bookmark>,
                val type: Book.Type,
                val useCoverReplacement: Boolean,
                val author: String?,
                val currentFile: File,
                val time: Int,
                val name: String,
                val chapters: ImmutableList<Chapter>,
                val playbackSpeed: Float,
                val root: String) : Comparable<Book> {

    override fun compareTo(other: Book): Int {
        return NaturalOrderComparator.STRING_COMPARATOR.compare(name, other.name)
    }

    companion object {
        const val ID_UNKNOWN = -1L
        const val SPEED_MAX = 3F
        const val SPEED_MIN = 0.5F
    }

    private val COVER_TRANSITION_PREFIX = "bookCoverTransition_"


    init {
        val chapterFiles = ArrayList<File>(chapters.size)
        for (c in chapters) {
            chapterFiles.add(c.file)
        }
        check(playbackSpeed >= SPEED_MIN, { "speed $playbackSpeed must be >= $SPEED_MIN" })
        check(playbackSpeed <= SPEED_MAX) { "speed $playbackSpeed must be <= $SPEED_MAX" }
        for (b in bookmarks) {
            check(chapterFiles.contains(b.mediaFile)) { "$chapterFiles does not contain the media file for $b" }
        }
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
        for (c in chapters) {
            globalDuration += c.duration
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
        val coverFile = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "data" + File.separator + App.component().context.packageName,
                id.toString() + ".jpg")
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
