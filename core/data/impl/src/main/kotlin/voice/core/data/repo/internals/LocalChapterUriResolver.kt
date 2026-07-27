package voice.core.data.repo.internals

import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import voice.core.data.BookContent
import voice.core.data.BookSource
import voice.core.data.Chapter
import voice.core.data.playback.ChapterUriResolver
import voice.core.data.toUri

@ContributesIntoSet(AppScope::class)
internal class LocalChapterUriResolver : ChapterUriResolver {

  override fun resolve(
    chapter: Chapter,
    content: BookContent,
  ): Uri? {
    if (content.source != BookSource.LOCAL) return null
    return chapter.id.toUri()
  }
}
