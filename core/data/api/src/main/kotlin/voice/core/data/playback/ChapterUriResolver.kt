package voice.core.data.playback

import android.net.Uri
import voice.core.data.BookContent
import voice.core.data.Chapter

public interface ChapterUriResolver {

  public fun resolve(
    chapter: Chapter,
    content: BookContent,
  ): Uri?
}
