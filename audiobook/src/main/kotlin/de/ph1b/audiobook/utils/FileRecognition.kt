package de.ph1b.audiobook.utils

import android.os.Build
import com.google.common.collect.Lists
import com.google.common.io.Files
import java.io.FileFilter
import java.util.*


/**
 * Class containing methods for recognizing different file types by their file ending.

 * @author Paul Woitaschek
 */
object FileRecognition {

    private val imageTypes = Arrays.asList("jpg", "jpeg", "png", "bmp")
    private val audioTypes: List<String>

    init {
        audioTypes = Lists.newArrayList("3gp",
                "aac",
                "awb",
                "flac",
                "imy",
                "m4a",
                "m4b",
                "mp4",
                "mid",
                "mkv",
                "mp3",
                "mp3package",
                "mxmf",
                "ogg",
                "oga",
                "ota",
                "rtttl",
                "rtx",
                "wav",
                "wma",
                "xmf"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTypes.add("opus")
        }
    }

    val folderAndMusicFilter = FileFilter {
        if (it.isDirectory) {
            return@FileFilter true
        } else {
            val extension = Files.getFileExtension(it.name)
                    .toLowerCase()
            return@FileFilter audioTypes.contains(extension)
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
