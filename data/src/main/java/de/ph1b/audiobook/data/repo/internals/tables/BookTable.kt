package de.ph1b.audiobook.data.repo.internals.tables

/**
 * Collection of strings representing the book table
 */
object BookTable {

  const val ID = "bookId"
  const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
  const val PLAYBACK_SPEED = "bookSpeed"
  const val TIME = "bookTime"
  const val ACTIVE = "BOOK_ACTIVE"
  const val LOUDNESS_GAIN = "loudnessGain"
  const val SKIP_SILENCE = "skipSilence"
  const val TABLE_NAME = "tableBooks"
  const val CREATE_TABLE = """
    CREATE TABLE $TABLE_NAME (
      $ID TEXT PRIMARY KEY NULL,
      $CURRENT_MEDIA_PATH TEXT NOT NULL,
      $PLAYBACK_SPEED REAL NOT NULL,
      $TIME INTEGER NOT NULL,
      $LOUDNESS_GAIN INTEGER,
      $SKIP_SILENCE INTEGER DEFAULT 0,
      $ACTIVE INTEGER NOT NULL DEFAULT 1
    )
  """
}
