package de.ph1b.audiobook.features.chapterReader

import com.coremedia.iso.IsoFile
import com.coremedia.iso.IsoTypeReader
import com.coremedia.iso.boxes.*
import com.coremedia.iso.boxes.mdat.MediaDataBox
import com.googlecode.mp4parser.boxes.apple.AppleLyricsBox
import com.googlecode.mp4parser.util.Path
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * Reads mp4 chapters
 *
 * @author Paul Woitaschek
 */
object Mp4ChapterReader {

  fun readChapters(file: File): Map<Long, String> {
    val isoFile = IsoFile(file.absolutePath)

    val fromChap = chap(isoFile)
    if (fromChap.isNotEmpty()) return fromChap

    val fromChpl = chpl(isoFile)
    if (fromChpl.isNotEmpty()) return fromChpl

    return lyr(isoFile)
  }

  private fun chap(isoFile: IsoFile): Map<Long, String> {
    val chapBoxData = Path.getPath<UnknownBox>(isoFile, "moov/trak/tref/chap")
        ?.data
        ?: return emptyMap()
    chapBoxData.rewind()

    val trackId = chapBoxData.int

    val trak = Path.getPaths<TrackBox>(isoFile, "moov/trak").firstOrNull {
      it.trackHeaderBox.trackId == trackId.toLong()
    } ?: return emptyMap()

    val timeScale = Path.getPath<MediaHeaderBox?>(trak, "mdia/mdhd")
        ?.timescale
        ?: return emptyMap()

    val durations = Path.getPath<TimeToSampleBox?>(trak, "mdia/minf/stbl/stts")
        ?.entries
        ?.map { it.count * 1000 / timeScale * it.delta }
        ?: return emptyMap()

    val mdat = Path.getPath<MediaDataBox?>(isoFile, "mdat[${trackId - 1}]")
        ?: return emptyMap()

    val outputStream = ByteArrayOutputStream()
    mdat.getBox(Channels.newChannel(outputStream))
    val byteBuffer = ByteBuffer.wrap(outputStream.toByteArray())

    byteBuffer.position(8)
    val names = ArrayList<String>()
    while (byteBuffer.hasRemaining()) {
      val textLength = byteBuffer.short.toInt()
      val textBytes = ByteArray(textLength)
      byteBuffer.get(textBytes)
      val name = String(textBytes)
      names.add(name)
      byteBuffer.position(byteBuffer.position() + 12)
    }

    if (names.size != durations.size || names.isEmpty())
      return emptyMap()

    val map = HashMap<Long, String>(names.size)
    var position = 0L
    names.forEachIndexed { index, name ->
      map.put(position, name)
      position += durations[index]
    }
    return map
  }

  private fun lyr(isoFile: IsoFile): Map<Long, String> {
    val lyr: AppleLyricsBox = Path.getPath(isoFile, "/moov/udta/meta/ilst/©lyr")
        ?: return emptyMap()

    val lines = lyr.value.split("\n")

    val map = HashMap<Long, String>()
    for (line in lines) {
      val split = line.split(delimiters = ' ', limit = 2)
      if (split.size != 2) continue

      val time = split[0]

      val dotSplit = time.split('.')
      if (dotSplit.isEmpty() || dotSplit.size > 2)
        continue

      // only hours
      val colorSplit = dotSplit.first().split(':')
      if (colorSplit.size != 3)
        continue

      val h = colorSplit[0].toLongOrNull()
          ?: continue
      val m = colorSplit[1].toLongOrNull()
          ?: continue
      val s = colorSplit[2].toLongOrNull()
          ?: continue
      val ms = dotSplit.getOrNull(1)
          ?.toLongOrNull()
          ?: 0

      val position = TimeUnit.HOURS.toMillis(h) + TimeUnit.MINUTES.toMillis(m) + TimeUnit.SECONDS.toMillis(s) + ms
      val name = split[1]

      map.put(position, name)
    }
    return map
  }

  private fun chpl(isoFile: IsoFile): Map<Long, String> {
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
