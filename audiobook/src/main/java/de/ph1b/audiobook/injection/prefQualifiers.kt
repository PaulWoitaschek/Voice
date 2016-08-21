package de.ph1b.audiobook.injection

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME


@Qualifier @Retention(RUNTIME)
annotation class ResumeOnReplug

@Qualifier @Retention(RUNTIME)
annotation class BookmarkOnSleepTimer

@Qualifier @Retention(RUNTIME)
annotation class AutoRewindAmount

@Qualifier @Retention(RUNTIME)
annotation class SeekTime

@Qualifier @Retention(RUNTIME)
annotation class PauseOnTempFocusLoss

@Qualifier @Retention(RUNTIME)
annotation class SleepTime

@Qualifier @Retention(RUNTIME)
annotation class SingleBookFolders

@Qualifier @Retention(RUNTIME)
annotation class CollectionFolders

@Qualifier @Retention(RUNTIME)
annotation class CurrentBookId