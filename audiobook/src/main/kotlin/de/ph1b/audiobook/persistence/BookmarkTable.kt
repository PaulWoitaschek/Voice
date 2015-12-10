package de.ph1b.audiobook.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.model.Bookmark

/**
 * Represents an sql table for bookmarks
 *
 * @author Paul Woitaschek
 */
object BookmarkTable {

    const val PATH = "bookmarkPath"
    const val TITLE = "bookmarkTitle"
    const val TABLE_NAME = "tableBookmarks"
    const val TIME = "bookmarkTime"
    const val BOOK_ID = "bookId"
    private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ( " +
            "  $PATH TEXT NOT NULL, " +
            "  $TITLE TEXT NOT NULL, " +
            "  $TIME INTEGER NOT NULL, " +
            "  $BOOK_ID INTEGER NOT NULL, " +
            "  FOREIGN KEY ( $BOOK_ID ) REFERENCES ${BookTable.TABLE_NAME} ( ${BookTable.ID} ) " +
            ")"

    fun getContentValues(bookmark: Bookmark, bookId: Long): ContentValues {
        val cv = ContentValues()
        cv.put(TIME, bookmark.time)
        cv.put(PATH, bookmark.mediaFile.absolutePath)
        cv.put(TITLE, bookmark.title)
        cv.put(BookTable.ID, bookId)
        return cv
    }

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
    }
}