package voice.playbackScreen

sealed class BookPlayViewEffect {
  object BookmarkAdded : BookPlayViewEffect()
  object ShowSleepTimeDialog : BookPlayViewEffect()
  data class ShowPlaybackSpeedDialog(val speed: Float) : BookPlayViewEffect()
}
