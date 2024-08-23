package voice.common.pref

import javax.inject.Qualifier

object PrefKeys {

  const val AUTO_REWIND_AMOUNT = "AUTO_REWIND"
  const val SEEK_TIME = "SEEK_TIME"
  const val SLEEP_TIME = "SLEEP_TIME"
  const val AUTO_SLEEP_TIMER = "AUTO_SLEEP_TIMER"
  const val SINGLE_BOOK_FOLDERS = "singleBookFolders"
  const val COLLECTION_BOOK_FOLDERS = "folders"
  const val DARK_THEME = "darkTheme"
  const val GRID_MODE = "gridView"
}

@Qualifier
annotation class OnboardingCompleted

@Qualifier
annotation class RootAudiobookFolders

@Qualifier
annotation class SingleFolderAudiobookFolders

@Qualifier
annotation class SingleFileAudiobookFolders

@Qualifier
annotation class AuthorAudiobookFolders

@Qualifier
annotation class CurrentBook
