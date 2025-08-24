package voice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recentBookSearch")
internal data class RecentBookSearch(
  @PrimaryKey
  val searchTerm: String,
)
