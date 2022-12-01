package voice.data

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import voice.common.comparator.NaturalOrderComparator

@Serializable(with = ChapterIdSerializer::class)
data class ChapterId(val value: String) : Comparable<ChapterId> {
  constructor(uri: Uri) : this(uri.toString())

  override fun compareTo(other: ChapterId): Int {
    return NaturalOrderComparator.uriComparator.compare(value.toUri(), other.value.toUri())
  }
}
