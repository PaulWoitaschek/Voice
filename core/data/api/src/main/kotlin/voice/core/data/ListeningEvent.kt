package voice.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

/**
 * Something that happened while listening to a book. The history turns these into stretches of
 * listening, so that a lost place can be found again.
 *
 * [chapterId] and [time] are where playback was when the event happened. For
 * jumps, [toChapterId] and [toTime] are where playback went.
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

    /**
     * Not recorded anymore. Only the nights from back when the sleep timer set a bookmark where it
     * started have it, and those are kept past the limit of events, unlike [SleepTimerStarted].
     */
    SleepTimerSet,
    SleepTimerStarted,
    SleepTimerEnded,
    SleepTimerExtended,
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

    /** The position came from another device, through the server or a synced library folder. */
    OtherDevice,
  }
}

public val ListeningEvent.at: Instant get() = Instant.ofEpochMilli(atMillis)

private val minimumJump = 30.seconds

/**
 * Seeks of more than 30 seconds, chapter changes and bookmark jumps can be accidents, so the player
 * offers to undo them and the history shows them. The rewind and fast forward buttons move only a
 * few seconds on purpose, so they don't count.
 */
public fun ListeningEvent.isUndoableJump(): Boolean {
  return when (type) {
    ListeningEvent.Type.ChapterChange,
    ListeningEvent.Type.BookmarkJump,
    -> true
    ListeningEvent.Type.Seek -> {
      val toChapterId = toChapterId
      val toTime = toTime
      toChapterId == null || toTime == null || isBigJump(chapterId, time, toChapterId, toTime)
    }
    else -> false
  }
}

public fun isBigJump(
  fromChapterId: ChapterId,
  fromTime: Long,
  toChapterId: ChapterId,
  toTime: Long,
): Boolean = toChapterId != fromChapterId || (toTime - fromTime).absoluteValue >= minimumJump.inWholeMilliseconds
