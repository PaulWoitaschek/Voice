package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import de.ph1b.audiobook.model.Chapter;

/**
 * Collection of strings representing the chapters table
 *
 * @author Paul Woitaschek
 */
class ChapterTable {
    static final String DURATION = "chapterDuration";
    static final String NAME = "chapterName";
    static final String PATH = "chapterPath";
    static final String TABLE_NAME = "tableChapters";
     static final String BOOK_ID = "bookId";
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
            DURATION + " INTEGER NOT NULL, " +
            NAME + " TEXT NOT NULL, " +
            PATH + " TEXT NOT NULL, " +
            BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + BookTable.TABLE_NAME + "(" + BookTable.ID + "))";

    public static ContentValues getContentValues(Chapter chapter, long bookId) {
        ContentValues chapterCv = new ContentValues();
        chapterCv.put(DURATION, chapter.duration());
        chapterCv.put(NAME, chapter.name());
        chapterCv.put(PATH, chapter.file().getAbsolutePath());
        chapterCv.put(BookTable.ID, bookId);
        return chapterCv;
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
