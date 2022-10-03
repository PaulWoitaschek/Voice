package voice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recentBookSearch")
data class RecentBookSearch(
  @PrimaryKey
  val searchTerm: String,
)
