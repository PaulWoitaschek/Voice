package voice.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.uuid.Uuid

// Listening sessions are stored as an event table so library-wide stats can be added later without touching collection logic.
@Entity(
  tableName = "listening_session",
  indices = [
    Index(value = ["startedAt"]),
    Index(value = ["bookId", "startedAt"]),
  ],
)
public data class ListeningSession(
  val bookId: BookId,
  val startedAt: Instant,
  val endedAt: Instant,
  val durationMs: Long,
  @PrimaryKey
  val id: Id,
) {

  // A dedicated id lets checkpoints overwrite the same segment while keeping separate rows for day-boundary splits.
  public data class Id(val value: Uuid) {
    public companion object {
      // Statistics segments need their own primary keys so checkpoints never collide with other real sessions.
      public fun random(): Id = Id(Uuid.random())
    }
  }

  init {
    // Validate time bounds before writing so the stats page never aggregates negative durations.
    require(!endedAt.isBefore(startedAt)) {
      "Listening session can't end before it starts: $this"
    }
    require(durationMs >= 0) {
      "Listening session duration can't be negative: $this"
    }
  }
}
