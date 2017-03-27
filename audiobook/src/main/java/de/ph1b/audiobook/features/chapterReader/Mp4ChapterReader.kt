package de.ph1b.audiobook.features.chapterReader

import com.coremedia.iso.IsoFile
import com.coremedia.iso.IsoTypeReader
import com.coremedia.iso.boxes.MovieHeaderBox
import com.coremedia.iso.boxes.UnknownBox
import com.googlecode.mp4parser.util.Path
import java.io.File

/**
 * Reads mp4 chapters
 *
 * @author Paul Woitaschek
 */
object Mp4ChapterReader {

  fun readChapters(file: File): Map<Long, String> {
    val isoFile = IsoFile(file.absolutePath)

    val mvhd: MovieHeaderBox = Path.getPath(isoFile, "/moov/mvhd")
        ?: return emptyMap()
    val timeScale = mvhd.timescale

    val chplBox: UnknownBox = Path.getPath(isoFile, "/moov/udta/chpl")
        ?: return emptyMap()
    val data = chplBox.data

    val count = data.get(8)
    data.position(9)
    val map = HashMap<Long, String>()
    (0 until count).forEach {
      val duration = IsoTypeReader.readUInt64(data) / timeScale / 10
      val titleSize = data.get()

      val titleBytes = ByteArray(titleSize.toInt())
      data.get(titleBytes, 0, titleBytes.size)
      val title = String(titleBytes)

      map.put(duration, title)
    }
    return map
  }
}
