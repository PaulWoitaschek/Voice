package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

/**
 * Makes headerless VBR MP3 files eligible for Media3's index seeker.
 *
 * Media3 otherwise assumes that a seekable, known-length MP3 without a Xing/VBRI table is CBR.
 * That produces a wrong duration when the file is actually VBR. The index seeker is selected when
 * the byte length is unknown and builds an exact time-to-byte map while reading the file.
 */
internal class IndexSeekingMp3DataSource(private val upstream: DataSource) : DataSource by upstream {

  override fun open(dataSpec: DataSpec): Long {
    val resolvedLength = upstream.open(dataSpec)
    return if (dataSpec.uri.lastPathSegment?.endsWith(".mp3", ignoreCase = true) == true) {
      C.LENGTH_UNSET.toLong()
    } else {
      resolvedLength
    }
  }
}
