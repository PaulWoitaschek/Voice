package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import de.ph1b.audiobook.model.Book;

/**
 * Collection of strings representing the book table
 *
 * @author Paul Woitaschek
 */
class BookTable {
    // book keys
    static final String BOOK_ID = "bookId";
    static final String BOOK_NAME = "bookName";
    static final String BOOK_AUTHOR = "bookAuthor";
    static final String BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath";
    static final String BOOK_PLAYBACK_SPEED = "bookSpeed";
    static final String BOOK_ROOT = "bookRoot";
    static final String BOOK_TIME = "bookTime";
    static final String BOOK_TYPE = "bookType";
    static final String BOOK_USE_COVER_REPLACEMENT = "bookUseCoverReplacement";
    static final String BOOK_ACTIVE = "BOOK_ACTIVE";
    static final String TABLE_BOOK = "tableBooks";
    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_NAME + " TEXT NOT NULL, " +
            BOOK_AUTHOR + " TEXT, " +
            BOOK_CURRENT_MEDIA_PATH + " TEXT NOT NULL, " +
            BOOK_PLAYBACK_SPEED + " REAL NOT NULL, " +
            BOOK_ROOT + " TEXT NOT NULL, " +
            BOOK_TIME + " INTEGER NOT NULL, " +
            BOOK_TYPE + " TEXT NOT NULL, " +
            BOOK_USE_COVER_REPLACEMENT + " INTEGER NOT NULL, " +
            BOOK_ACTIVE + " INTEGER NOT NULL DEFAULT 1)";


    public static ContentValues getContentValues(Book book) {
        ContentValues bookCv = new ContentValues();
        bookCv.put(BookTable.BOOK_NAME, book.getName());
        bookCv.put(BookTable.BOOK_AUTHOR, book.getAuthor());
        bookCv.put(BookTable.BOOK_ACTIVE, 1);
        bookCv.put(BookTable.BOOK_CURRENT_MEDIA_PATH, book.getCurrentFile().getAbsolutePath());
        bookCv.put(BookTable.BOOK_PLAYBACK_SPEED, book.getPlaybackSpeed());
        bookCv.put(BookTable.BOOK_ROOT, book.getRoot());
        bookCv.put(BookTable.BOOK_TIME, book.getTime());
        bookCv.put(BookTable.BOOK_TYPE, book.getType().name());
        bookCv.put(BookTable.BOOK_USE_COVER_REPLACEMENT, book.isUseCoverReplacement());
        return bookCv;
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOK);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
    }
}
