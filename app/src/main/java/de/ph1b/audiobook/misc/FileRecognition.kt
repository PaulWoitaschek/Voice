package de.ph1b.audiobook.misc

import java.io.FileFilter
import java.util.*

/**
 * Class containing methods for recognizing different file types by their file ending.
 */
object FileRecognition {

  private val imageTypes = Arrays.asList("jpg", "jpeg", "png", "bmp")
  private val audioTypes = arrayOf(
    "3gp",
    "aac",
    "awb",
    "flac",
    "imy",
    "m4a",
    "m4b",
    "mid",
    "mka",
    "mkv",
    "mp3",
    "mp3package",
    "mp4",
    "opus",
    "mxmf",
    "oga",
    "ogg",
    "ota",
    "rtttl",
    "rtx",
    "wav",
    "webm",
    "wma",
    "xmf"
  )

  val imageFilter = FileFilter {
    val extension = it.extension.toLowerCase()
    imageTypes.contains(extension)
  }

  val musicFilter = FileFilter {
    val extension = it.extension.toLowerCase()
    audioTypes.contains(extension)
  }

  val folderAndMusicFilter = FileFilter {
    if (it.isDirectory) {
      true
    } else {
      val extension = it.extension
        .toLowerCase()
      audioTypes.contains(extension)
    }
  }
}
