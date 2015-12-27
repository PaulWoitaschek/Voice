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

import com.google.common.io.Files
import org.videolan.libvlc.util.Extensions
import java.io.FileFilter
import java.util.*


/**
 * Class containing methods for recognizing different file types by their file ending.

 * @author Paul Woitaschek
 */
object FileRecognition {

    private val imageTypes = Arrays.asList("jpg", "jpeg", "png", "bmp")

    val folderAndMusicFilter = FileFilter {
        if (it.isDirectory) {
            return@FileFilter true
        } else {
            val extension = Files.getFileExtension(it.name)
                    .toLowerCase()
            return@FileFilter Extensions.AUDIO.contains(extension)
        }
    }
    val folderAndImagesFilter = FileFilter {
        if (it.isDirectory) {
            return@FileFilter true
        } else {
            val extension = Files.getFileExtension(it.name)
                    .toLowerCase()
            return@FileFilter imageTypes.contains(extension)
        }
    }
}
