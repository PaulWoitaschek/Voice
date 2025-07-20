package voice.common.pref

import dev.zacsweers.metro.Qualifier

@Qualifier
annotation class OnboardingCompletedStore

@Qualifier
annotation class RootAudiobookFoldersStore

@Qualifier
annotation class SingleFolderAudiobookFoldersStore

@Qualifier
annotation class SingleFileAudiobookFoldersStore

@Qualifier
annotation class AuthorAudiobookFoldersStore

@Qualifier
annotation class CurrentBookStore

@Qualifier
annotation class AutoRewindAmountStore

@Qualifier
annotation class SeekTimeStore

@Qualifier
annotation class SleepTimerPreferenceStore

@Qualifier
annotation class GridModeStore

@Qualifier
annotation class DarkThemeStore

@Qualifier
annotation class FadeOutStore
