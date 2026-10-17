package voice.features.bookmark

import androidx.compose.runtime.Immutable
import voice.core.data.Bookmark
import voice.core.ui.BookBarPin

@Immutable
internal data class BookmarkViewState(
  val bookTitle: String,
  val bookAuthor: String?,
  val cover: String?,
  val playing: Boolean,
  val sleepTimerActive: Boolean,
  val bookBar: BookBarViewState?,
  val sort: BookmarkSort,
  val categories: List<CategoryCount>,
  val selectedCategory: Bookmark.Kind?,
  val items: List<BookmarkListItem>,
  val totalCount: Int,
  val editor: BookmarkEditorViewState?,
  val tab: BookmarkTab,
)

internal enum class BookmarkTab {
  Bookmarks,
  History,
}

@Immutable
internal data class BookBarViewState(
  val segments: List<Float>,
  val currentSegment: Int,
  val progress: Float,
  val percent: Int,
  val remaining: String,
  val pins: List<BookBarPin>,
  /** For each pin, the key of its row in the list. */
  val pinKeys: List<String>,
)

internal enum class BookmarkSort {
  Recent,
  Story,
}

internal data class CategoryCount(
  val kind: Bookmark.Kind,
  val count: Int,
)

/** Where an item sits in its group of connected items. */
internal data class GroupPosition(
  val index: Int,
  val count: Int,
)

@Immutable
internal sealed interface BookmarkListItem {

  val key: String

  data class ChapterHeader(
    val number: Int,
    val name: String?,
  ) : BookmarkListItem {
    override val key: String get() = "chapter-$number"
  }

  data class Row(
    val bookmark: BookmarkRowViewState,
    val group: GroupPosition,
  ) : BookmarkListItem {
    override val key: String get() = bookmark.id.value.toString()
  }

  data class YouAreHere(
    val time: String,
    val percent: Int,
    val group: GroupPosition,
  ) : BookmarkListItem {
    override val key: String get() = YOU_ARE_HERE_KEY
  }
}

internal const val YOU_ARE_HERE_KEY = "you-are-here"

@Immutable
internal data class BookmarkRowViewState(
  val id: Bookmark.Id,
  val kind: Bookmark.Kind,
  val note: String?,
  val chapterNumber: Int,
  val chapterName: String?,
  val time: String,
  val percent: Int,
  val savedAt: DayLabel,
  /** In story order the chapter is in the header above, otherwise the row names it. */
  val showChapter: Boolean,
)

@Immutable
internal data class BookmarkEditorViewState(
  val id: Bookmark.Id,
  val isNew: Boolean,
  val note: String,
  val kind: Bookmark.Kind,
  val chapterNumber: Int,
  val chapterName: String?,
  /** False for a book with a single chapter. */
  val showChapter: Boolean,
  val time: String,
  val percent: Int,
  val canMoveEarlier: Boolean,
  val canMoveLater: Boolean,
)

internal sealed interface BookmarkViewEffect {
  data class Deleted(val bookmark: Bookmark) : BookmarkViewEffect
}
