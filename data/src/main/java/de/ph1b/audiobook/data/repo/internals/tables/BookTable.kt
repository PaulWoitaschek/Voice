package de.ph1b.audiobook.data.repo.internals.tables

/**
 * Collection of strings representing the book table
 */
object BookTable {

  const val ID = "bookId"
  const val NAME = "bookName"
  const val AUTHOR = "bookAuthor"
  const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
  const val PLAYBACK_SPEED = "bookSpeed"
  const val ROOT = "bookRoot"
  const val TIME = "bookTime"
  const val TYPE = "bookType"
  const val ACTIVE = "BOOK_ACTIVE"
  const val LOUDNESS_GAIN = "loudnessGain"
  const val TABLE_NAME = "tableBooks"
  const val CREATE_TABLE = """
    CREATE TABLE ${TABLE_NAME} (
      ${ID} INTEGER PRIMARY KEY AUTOINCREMENT,
      ${NAME} TEXT NOT NULL,
      ${AUTHOR} TEXT,
      ${CURRENT_MEDIA_PATH} TEXT NOT NULL,
      ${PLAYBACK_SPEED} REAL NOT NULL,
      ${ROOT} TEXT NOT NULL,
      ${TIME} INTEGER NOT NULL,
      ${TYPE} TEXT NOT NULL,
      ${LOUDNESS_GAIN} INTEGER,
      ${ACTIVE} INTEGER NOT NULL DEFAULT 1
    )
  """
}
