package voice.playbackScreen

sealed class BookPlayViewEffect {
  object BookmarkAdded : BookPlayViewEffect()
  object ShowSleepTimeDialog : BookPlayViewEffect()
}
