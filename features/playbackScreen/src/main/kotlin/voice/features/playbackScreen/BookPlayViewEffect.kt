package voice.features.playbackScreen

import voice.core.data.Bookmark

internal sealed interface BookPlayViewEffect {
  data class BookmarkAdded(val id: Bookmark.Id) : BookPlayViewEffect
  data object RequestIgnoreBatteryOptimization : BookPlayViewEffect
}
