package voice.core.data

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable(with = ChapterIdSerializer::class)
public data class ChapterId(val value: String) {
  public constructor(uri: Uri) : this(uri.toString())
}
