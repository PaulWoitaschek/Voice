package de.ph1b.audiobook.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

import de.ph1b.audiobook.model.Chapter

/**
 * Collection of strings representing the chapters table

 * @author Paul Woitaschek
 */
internal object ChapterTable {
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

    fun getContentValues(chapter: Chapter, bookId: Long): ContentValues {
        val chapterCv = ContentValues()
        chapterCv.put(DURATION, chapter.duration)
        chapterCv.put(NAME, chapter.name)
        chapterCv.put(PATH, chapter.file.absolutePath)
        chapterCv.put(BookTable.ID, bookId)
        return chapterCv
    }

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
    }
}
