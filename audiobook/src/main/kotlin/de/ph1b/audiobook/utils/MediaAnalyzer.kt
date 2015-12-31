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

package de.ph1b.audiobook.utils

import android.media.MediaMetadataRetriever
import com.google.common.io.Files
import java.io.File
import javax.inject.Inject

/**
 * Simple class for analyzing media files and finding information about their metadata.
 *
 * @author Paul Woitaschek
 */
class MediaAnalyzer
@Inject constructor() {

    /**
     * As [MediaMetadataRetriever] has several bugs it is important to catch the exception here as
     * it randomly throws [RuntimeException] on certain implementations.
     */
    private fun MediaMetadataRetriever.safeExtract(key: Int): String? {
        return try {
            extractMetadata(key)
        } catch(ignored: Exception) {
            null
        }
    }

    private fun String.toIntOrDefault(default: Int): Int {
        return try {
            toInt()
        } catch(ignored: NumberFormatException) {
            default
        }
    }

    fun compute(input: File): Result {
        // Note: MediaMetadataRetriever throws undocumented RuntimeExceptions. We catch these
        // and act appropriate.
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(input.absolutePath)

            val parsedDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = parsedDuration?.toIntOrDefault(-1) ?: -1

            // getting chapter-name
            var chapterName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_TITLE)
            // checking for dot index because otherwise a file called ".mp3" would have no name.
            if (chapterName.isNullOrEmpty()) {
                val fileName = Files.getNameWithoutExtension(input.absolutePath)!!
                chapterName = if (fileName.isEmpty()) input.name else fileName
            }

            var author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (author.isNullOrEmpty())
                author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)

            val bookName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            return Result(duration, chapterName!!, author, bookName)
        } catch(ignored: RuntimeException) {
            return Result(-1, "Chapter", null, null)
        } finally {
            mmr.release()
        }
    }

    data class Result(val duration: Int, val chapterName: String, val author: String?, val bookName: String?)
}

