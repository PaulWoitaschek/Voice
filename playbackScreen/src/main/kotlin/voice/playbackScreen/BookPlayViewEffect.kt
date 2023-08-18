package voice.playbackScreen

internal sealed interface BookPlayViewEffect {
  data object BookmarkAdded : BookPlayViewEffect
  data object RequestIgnoreBatteryOptimization : BookPlayViewEffect
}
