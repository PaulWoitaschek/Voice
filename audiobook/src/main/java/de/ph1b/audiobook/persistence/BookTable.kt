package de.ph1b.audiobook.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

import de.ph1b.audiobook.model.Book

/**
 * Collection of strings representing the book table

 * @author Paul Woitaschek
 */
internal object BookTable {
    const val ID = "bookId"
    const val NAME = "bookName"
    const val AUTHOR = "bookAuthor"
    const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
    const val PLAYBACK_SPEED = "bookSpeed"
    const val ROOT = "bookRoot"
    const val TIME = "bookTime"
    const val TYPE = "bookType"
    const val USE_COVER_REPLACEMENT = "bookUseCoverReplacement"
    const val ACTIVE = "BOOK_ACTIVE"
    const val TABLE_NAME = "tableBooks"
    const private val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ( " +
            "  $ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "  $NAME TEXT NOT NULL, " +
            "  $AUTHOR TEXT, " +
            "  $CURRENT_MEDIA_PATH TEXT NOT NULL, " +
            "  $PLAYBACK_SPEED REAL NOT NULL, " +
            "  $ROOT TEXT NOT NULL, " +
            "  $TIME INTEGER NOT NULL, " +
            "  $TYPE TEXT NOT NULL, " +
            "  $USE_COVER_REPLACEMENT INTEGER NOT NULL, " +
            "  $ACTIVE INTEGER NOT NULL DEFAULT 1" +
            ")"

    fun getContentValues(book: Book): ContentValues {
        val bookCv = ContentValues()
        bookCv.put(BookTable.NAME, book.name)
        bookCv.put(BookTable.AUTHOR, book.author)
        bookCv.put(BookTable.ACTIVE, 1)
        bookCv.put(BookTable.CURRENT_MEDIA_PATH, book.currentFile.absolutePath)
        bookCv.put(BookTable.PLAYBACK_SPEED, book.playbackSpeed)
        bookCv.put(BookTable.ROOT, book.root)
        bookCv.put(BookTable.TIME, book.time)
        bookCv.put(BookTable.TYPE, book.type.name)
        bookCv.put(BookTable.USE_COVER_REPLACEMENT, book.useCoverReplacement)
        return bookCv
    }

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
    }
}
