package voice.core.scanner

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.CustomMetadataRepo
import voice.core.documentfile.CachedDocumentFile

public data class BookPreview(
  val name: String,
  val author: String?,
  val duration: Duration,
)

// the analyzed files end up in the chapter cache, so the scan after adding the folder only puts the
// books together
public fun interface BookPreviewer {

  public suspend fun preview(file: CachedDocumentFile): BookPreview?
}

@ContributesBinding(AppScope::class)
@Inject
public class BookPreviewerImpl
internal constructor(
  private val contentRepo: BookContentRepo,
  private val customMetadataRepo: CustomMetadataRepo,
  private val chapterParser: ChapterParser,
  private val bookParser: BookParser,
) : BookPreviewer {

  override suspend fun preview(file: CachedDocumentFile): BookPreview? {
    val parseResult = chapterParser.parse(file)
    val chapters = parseResult.chapters
    if (chapters.isEmpty()) return null
    val id = BookId(file.uri)
    val metadata =
      (contentRepo.get(id) ?: bookParser.parse(chapters, file, parseResult.firstChapterMetadata))
        .metadata
    // a book that is already in the library keeps the names the listener gave it
    val custom = customMetadataRepo.get(id)
    return BookPreview(
      name = custom.name ?: metadata.name,
      author = custom.author ?: metadata.author,
      duration = chapters.sumOf { it.duration }.milliseconds,
    )
  }
}
