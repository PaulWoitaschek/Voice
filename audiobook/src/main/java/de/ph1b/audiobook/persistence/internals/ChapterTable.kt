package de.ph1b.audiobook.persistence.internals

import android.database.sqlite.SQLiteDatabase

/**
 * Collection of strings representing the chapters table

 * @author Paul Woitaschek
 */
object ChapterTable {
    const val DURATION = "chapterDuration"
    const val NAME = "chapterName"
    const val PATH = "chapterPath"
    const val TABLE_NAME = "tableChapters"
    const val BOOK_ID = "bookId"
    private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ( " +
            "  $DURATION INTEGER NOT NULL, " +
            "  $NAME TEXT NOT NULL, " +
            "  $PATH TEXT NOT NULL, " +
            "  $BOOK_ID INTEGER NOT NULL, " +
            "  FOREIGN KEY ( $BOOK_ID ) REFERENCES ${BookTable.TABLE_NAME} ( ${BookTable.ID} )" +
            " )"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
    }
}
