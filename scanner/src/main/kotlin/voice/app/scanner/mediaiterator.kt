package voice.app.scanner

import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.TrackGroup
import androidx.media3.exoplayer.source.TrackGroupArray

operator fun TrackGroupArray.iterator(): Iterator<TrackGroup> = object : Iterator<TrackGroup> {
  private var index = 0
  override fun hasNext(): Boolean = index < length
  override fun next(): TrackGroup {
    if (!hasNext()) throw NoSuchElementException()
    return get(index++)
  }
}

operator fun TrackGroup.iterator() = object : Iterator<Format> {
  private var index = 0
  override fun hasNext(): Boolean = index < length
  override fun next(): Format {
    if (!hasNext()) throw NoSuchElementException()
    return getFormat(index++)
  }
}

operator fun Metadata.iterator() = object : Iterator<Metadata.Entry> {
  private var index = 0
  override fun hasNext(): Boolean = index < length()
  override fun next(): Metadata.Entry {
    if (!hasNext()) throw NoSuchElementException()
    return get(index++)
  }
}
