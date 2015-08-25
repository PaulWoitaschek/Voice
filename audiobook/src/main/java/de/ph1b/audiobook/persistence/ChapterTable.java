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
    static final String CHAPTER_DURATION = "chapterDuration";
    static final String CHAPTER_NAME = "chapterName";
    static final String CHAPTER_PATH = "chapterPath";
    static final String TABLE_CHAPTERS = "tableChapters";
    private static final String CREATE_TABLE_CHAPTERS = "CREATE TABLE " + TABLE_CHAPTERS + " ( " +
            CHAPTER_DURATION + " INTEGER NOT NULL, " +
            CHAPTER_NAME + " TEXT NOT NULL, " +
            CHAPTER_PATH + " TEXT NOT NULL, " +
            BookTable.BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BookTable.BOOK_ID + ") REFERENCES " + BookTable.TABLE_BOOK + "(" + BookTable.BOOK_ID + "))";

    public static ContentValues getContentValues(Chapter chapter, long bookId) {
        ContentValues chapterCv = new ContentValues();
        chapterCv.put(CHAPTER_DURATION, chapter.getDuration());
        chapterCv.put(CHAPTER_NAME, chapter.getName());
        chapterCv.put(CHAPTER_PATH, chapter.getFile().getAbsolutePath());
        chapterCv.put(BookTable.BOOK_ID, bookId);
        return chapterCv;
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CHAPTERS);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
    }
}
