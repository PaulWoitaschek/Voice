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
    static final String BOOKMARK_PATH = "bookmarkPath";
    static final String BOOKMARK_TITLE = "bookmarkTitle";
    static final String TABLE_BOOKMARKS = "tableBookmarks";
    static final String BOOKMARK_TIME = "bookmarkTime";
    private static final String CREATE_TABLE_BOOKMARKS = "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
            BOOKMARK_PATH + " TEXT NOT NULL, " +
            BOOKMARK_TITLE + " TEXT NOT NULL, " +
            BOOKMARK_TIME + " INTEGER NOT NULL, " +
            BookTable.BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BookTable.BOOK_ID + ") REFERENCES " + BookTable.TABLE_BOOK + "(" + BookTable.BOOK_ID + "))";

    static ContentValues getContentValues(Bookmark bookmark, long bookId) {
        ContentValues cv = new ContentValues();
        cv.put(BOOKMARK_TIME, bookmark.getTime());
        cv.put(BOOKMARK_PATH, bookmark.getMediaFile().getAbsolutePath());
        cv.put(BOOKMARK_TITLE, bookmark.getTitle());
        cv.put(BookTable.BOOK_ID, bookId);
        return cv;
    }


    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOKMARKS);
    }

    public static void dropTableIfExists(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
    }
}
