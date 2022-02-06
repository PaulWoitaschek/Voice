package de.ph1b.audiobook.common.pref

import javax.inject.Qualifier

object PrefKeys {

  const val RESUME_ON_REPLUG = "RESUME_ON_REPLUG"
  const val AUTO_REWIND_AMOUNT = "AUTO_REWIND"
  const val SEEK_TIME = "SEEK_TIME"
  const val SLEEP_TIME = "SLEEP_TIME"
  const val SINGLE_BOOK_FOLDERS = "singleBookFolders"
  const val COLLECTION_BOOK_FOLDERS = "folders"
  const val DARK_THEME = "darkTheme"
  const val GRID_MODE = "gridView"
}

@Qualifier
annotation class AudiobookFolders

@Qualifier
annotation class CurrentBook
