package voice.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Something that happened while listening to a book, stored so that accidental
 * plays, pauses and jumps can be found and undone.
 *
 * [chapterId] and [time] are where playback was when the event happened. For
 * jumps, [toChapterId] and [toTime] are where playback went.
 *
 * Bookmark events also store the bookmark, so that a deleted one can be restored as it was.
 */
@Entity(
  tableName = "listening_event",
  indices = [Index(value = ["bookId", "atMillis"])],
)
public data class ListeningEvent(
  val bookId: BookId,
  val type: Type,
  val source: Source,
  val atMillis: Long,
  val chapterId: ChapterId,
  val time: Long,
  val toChapterId: ChapterId? = null,
  val toTime: Long? = null,
  val value: String? = null,
  val sourcePackage: String? = null,
  val bookmarkId: Bookmark.Id? = null,
  val bookmarkKind: Bookmark.Kind? = null,
  val bookmarkSetBySleepTimer: Boolean? = null,
  val bookmarkAddedAtMillis: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
) {

  public enum class Type {
    Play,
    Pause,
    Seek,
    SkipBack,
    SkipForward,
    ChapterChange,
    BookmarkJump,
    JumpBack,
    SpeedChanged,
    SkipSilenceChanged,
    VolumeBoostChanged,
    SleepTimerSet,
    SleepTimerEnded,
    SleepTimerExtended,
    BookmarkAdded,
    BookmarkDeleted,
  }

  public enum class Source {
    App,
    Widget,
    Notification,
    Headset,
    Bluetooth,
    Car,
    Watch,
    OtherApp,
    AudioFocus,
    Unplugged,
    SleepTimer,
    Unknown,
  }
}

public val ListeningEvent.at: Instant get() = Instant.ofEpochMilli(atMillis)
