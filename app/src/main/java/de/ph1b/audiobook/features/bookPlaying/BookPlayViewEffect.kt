package de.ph1b.audiobook.features.bookPlaying

sealed class BookPlayViewEffect {
  object BookmarkAdded : BookPlayViewEffect()
  object ShowSleepTimeDialog : BookPlayViewEffect()
}
