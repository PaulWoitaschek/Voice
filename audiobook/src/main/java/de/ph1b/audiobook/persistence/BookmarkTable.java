package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import de.ph1b.audiobook.model.Bookmark;

/**
 * Collection of strings representing the bookmark table
 *
 * @author Paul Woitaschek
 */
class BookmarkTable {
    static final String PATH = "bookmarkPath";
    static final String TITLE = "bookmarkTitle";
    static final String TABLE_NAME = "tableBookmarks";
    static final String TIME = "bookmarkTime";
    private static final String BOOK_ID = "bookId";
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
            PATH + " TEXT NOT NULL, " +
            TITLE + " TEXT NOT NULL, " +
            TIME + " INTEGER NOT NULL, " +
            BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + BookTable.TABLE_NAME + "(" + BookTable.ID + "))";

    static ContentValues getContentValues(Bookmark bookmark, long bookId) {
        ContentValues cv = new ContentValues();
        cv.put(TIME, bookmark.getTime());
        cv.put(PATH, bookmark.getMediaFile().getAbsolutePath());
        cv.put(TITLE, bookmark.getTitle());
        cv.put(BookTable.ID, bookId);
        return cv;
    }


    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
