package voice.playbackScreen

internal sealed interface BookPlayViewEffect {
  object BookmarkAdded : BookPlayViewEffect
  object ShowSleepTimeDialog : BookPlayViewEffect
  object RequestIgnoreBatteryOptimization : BookPlayViewEffect
}
