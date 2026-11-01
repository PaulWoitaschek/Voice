package voice.core.scanner

import androidx.media3.common.Metadata.Entry
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import voice.core.data.MarkData
import voice.core.logging.api.Logger

internal fun Metadata.Builder.read(entry: Entry) {
  when (entry) {
    is TextInformationFrame -> readText(entry)
    is BinaryFrame -> readBinary(entry)
    is InternalFrame -> tag(entry.description, entry.text)
    is ChapterFrame -> readChapter(entry)
    is VorbisComment -> readVorbis(entry)
    is MdtaMetadataEntry -> readMdta(entry)
  }
}

private fun Metadata.Builder.readMdta(entry: MdtaMetadataEntry) {
  val value = entry.value.toString(Charsets.UTF_8)
  when (entry.key) {
    "com.apple.quicktime.title" -> title = value
    "com.apple.quicktime.artist" -> artist = value
    "com.apple.quicktime.album" -> album = value
    else ->
      if (entry.typeIndicator == MdtaMetadataEntry.TYPE_INDICATOR_STRING) tag(entry.key, value)
  }
}

private fun Metadata.Builder.readVorbis(entry: VorbisComment) {
  val key = entry.key
  val value = entry.value
  when {
    key == "ARTIST" -> artist = value
    key == "ALBUM" -> album = value
    key == "TITLE" -> title = value
    key.startsWith("CHAPTER") -> {
      val withoutPrefix = key.removePrefix("CHAPTER")
      val isName = withoutPrefix.endsWith("NAME")
      if (isName) {
        val index = withoutPrefix.removeSuffix("NAME").toIntOrNull()
        if (index != null) {
          vorbisChapterNames[index] = value
        }
      } else {
        val index = withoutPrefix.toIntOrNull()
        if (index != null) {
          val duration = parseVorbisDuration(value)
          if (duration != null) {
            vorbisChapterStarts[index] = duration.inWholeMilliseconds
          }
        }
      }
    }
    else -> tag(key, value)
  }
}

private fun Metadata.Builder.readChapter(entry: ChapterFrame) {
  repeat(entry.subFrameCount) { subFrameIndex ->
    val subFrame = entry.getSubFrame(subFrameIndex)
    if (subFrame is TextInformationFrame) {
      chapters.add(MarkData(startMs = entry.startTimeMs.toLong(), name = subFrame.values.first()))
    }
  }
}

private fun Metadata.Builder.readText(entry: TextInformationFrame) {
  val value = entry.values.first()
  when (entry.id) {
    "TIT2" -> title = value
    "TPE1" -> artist = value
    "TALB" -> album = value
    "TCON" -> genre = value
    "TCOM" -> narrator = value
    "TXXX" ->
      when (val description = entry.description) {
        "TXXX:NARRATOR" -> narrator = if (narrator.isNullOrBlank()) value else narrator
        null -> {}
        else -> tag(description, value)
      }
    // media3 names MP4 atoms like ID3 frames: ©mvn is MVNM, tvsh is TVSHOW and ©grp is TIT1
    "MVNM",
    "MVIN",
    "TIT1" -> tag(entry.id, value)
    "TVSHOW" -> tag("SHOW", value)
    "TRCK",
    "TYER",
    "TSSE" -> {}
    else -> Logger.v("Unknown frame ID: ${entry.id}")
  }
}

// MP3 frames media3 doesn't know arrive as bytes
private fun Metadata.Builder.readBinary(entry: BinaryFrame) {
  if (entry.id in setOf("MVNM", "MVIN", "GRP1")) {
    entry.data.id3Text()?.let { tag(entry.id, it) }
  }
}

private fun ByteArray.id3Text(): String? {
  if (isEmpty()) return null
  val charset =
    when (this[0].toInt()) {
      0 -> Charsets.ISO_8859_1
      1 -> Charsets.UTF_16
      2 -> Charsets.UTF_16BE
      3 -> Charsets.UTF_8
      else -> return null
    }
  return String(this, 1, size - 1, charset).substringBefore('\u0000')
}
