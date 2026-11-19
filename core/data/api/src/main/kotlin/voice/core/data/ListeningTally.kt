package voice.core.data

import androidx.room.Entity
import java.time.LocalDate

@Entity(tableName = "listening_tally", primaryKeys = ["day", "bookId", "name"])
public data class ListeningTally(
  val day: LocalDate,
  val bookId: BookId,
  // a text and not a column for each number, so that a new number needs no migration
  val name: String,
  val number: Long,
)
