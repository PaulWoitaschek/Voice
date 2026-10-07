package voice.core.scanner

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.documentfile.CachedDocumentFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * What a book will look like in the library, worked out before its folder is added.
 */
public data class BookPreview(
  val name: String,
  val author: String?,
  val duration: Duration,
)

/**
 * Analyzes a book before its folder is added, the same way a scan does. The analyzed files end up in the
 * chapter cache, so the scan after adding the folder only needs to put the books together.
 */
public fun interface BookPreviewer {

  /** Returns `null` if the book has no playable files, the scan would skip it as well. */
  public suspend fun preview(file: CachedDocumentFile): BookPreview?
}

@ContributesBinding(AppScope::class)
@Inject
public class BookPreviewerImpl
internal constructor(
  private val contentRepo: BookContentRepo,
  private val chapterParser: ChapterParser,
  private val bookParser: BookParser,
) : BookPreviewer {

  override suspend fun preview(file: CachedDocumentFile): BookPreview? {
    val parseResult = chapterParser.parse(file)
    val chapters = parseResult.chapters
    if (chapters.isEmpty()) return null
    // a book that is already in the library keeps its name and author, like in the scan
    val content = contentRepo.get(BookId(file.uri))
      ?: bookParser.parse(chapters, file, parseResult.firstChapterMetadata)
    return BookPreview(
      name = content.name,
      author = content.author,
      duration = chapters.sumOf { it.duration }.milliseconds,
    )
  }
}
