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
    static final String ID = "bookId";
    static final String NAME = "bookName";
    static final String AUTHOR = "bookAuthor";
    static final String CURRENT_MEDIA_PATH = "bookCurrentMediaPath";
    static final String PLAYBACK_SPEED = "bookSpeed";
    static final String ROOT = "bookRoot";
    static final String TIME = "bookTime";
    static final String TYPE = "bookType";
    static final String USE_COVER_REPLACEMENT = "bookUseCoverReplacement";
    static final String ACTIVE = "BOOK_ACTIVE";
    static final String TABLE_NAME = "tableBooks";
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
            ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            NAME + " TEXT NOT NULL, " +
            AUTHOR + " TEXT, " +
            CURRENT_MEDIA_PATH + " TEXT NOT NULL, " +
            PLAYBACK_SPEED + " REAL NOT NULL, " +
            ROOT + " TEXT NOT NULL, " +
            TIME + " INTEGER NOT NULL, " +
            TYPE + " TEXT NOT NULL, " +
            USE_COVER_REPLACEMENT + " INTEGER NOT NULL, " +
            ACTIVE + " INTEGER NOT NULL DEFAULT 1)";


    public static ContentValues getContentValues(Book book) {
        ContentValues bookCv = new ContentValues();
        bookCv.put(BookTable.NAME, book.getName());
        bookCv.put(BookTable.AUTHOR, book.getAuthor());
        bookCv.put(BookTable.ACTIVE, 1);
        bookCv.put(BookTable.CURRENT_MEDIA_PATH, book.getCurrentFile().getAbsolutePath());
        bookCv.put(BookTable.PLAYBACK_SPEED, book.getPlaybackSpeed());
        bookCv.put(BookTable.ROOT, book.getRoot());
        bookCv.put(BookTable.TIME, book.getTime());
        bookCv.put(BookTable.TYPE, book.getType().name());
        bookCv.put(BookTable.USE_COVER_REPLACEMENT, book.isUseCoverReplacement());
        return bookCv;
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
