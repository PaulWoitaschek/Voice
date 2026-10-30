package voice.core.audiobookshelf

import kotlinx.serialization.json.Json

internal val audiobookshelfJson = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
  coerceInputValues = true
}
