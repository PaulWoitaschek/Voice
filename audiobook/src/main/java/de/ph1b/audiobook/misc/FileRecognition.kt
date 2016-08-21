package de.ph1b.audiobook.misc

import android.os.Build
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
        audioTypes = arrayListOf("3gp",
                "aac",
                "awb",
                "flac",
                "imy",
                "m4a",
                "m4b",
                "mp4",
                "mid",
                "mka",
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
                "webm",
                "wma",
                "xmf"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTypes.add("opus")
        }
    }

    val imageFilter = FileFilter {
        val extension = it.extension.toLowerCase()
        return@FileFilter imageTypes.contains(extension)
    }

    val musicFilter = FileFilter {
        val extension = it.extension.toLowerCase()
        return@FileFilter audioTypes.contains(extension)
    }

    val folderAndMusicFilter = FileFilter {
        if (it.isDirectory) {
            return@FileFilter true
        } else {
            val extension = it.extension
                    .toLowerCase()
            return@FileFilter audioTypes.contains(extension)
        }
    }
}