package de.ph1b.audiobook.scanner

import java.io.FileFilter
import java.util.Locale

/**
 * Class containing methods for recognizing different file types by their file ending.
 */
object FileRecognition {

  private val imageTypes = listOf("jpg", "jpeg", "png", "bmp")
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
    val extension = it.extension.toLowerCase(Locale.US)
    extension in imageTypes
  }

  val musicFilter = FileFilter {
    val extension = it.extension.toLowerCase(Locale.US)
    extension in audioTypes
  }

  val folderAndMusicFilter = FileFilter {
    if (it.isDirectory) {
      true
    } else {
      val extension = it.extension.toLowerCase(Locale.US)
      extension in audioTypes
    }
  }
}
