package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.isNotEmpty
import java.io.File

/**
 * Reads mp4 chapters
 *
 * @author Paul Woitaschek
 */
object Mp4ChapterReader {

  fun readChapters(file: File): SparseArray<String> {
    val fromChap = ChapReader.read(file)
    if (fromChap.isNotEmpty()) return fromChap

    return ChplReader.read(file)
  }
}
