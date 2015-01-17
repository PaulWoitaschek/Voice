package de.ph1b.audiobook.content;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.utils.ImageHelper;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "audioBookDB";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_MEDIA = "TABLE_MEDIA";
    private static final String TABLE_BOOKS = "TABLE_BOOKS";
    private static final String TABLE_BOOKMARKS = "TABLE_BOOKMARKS";

    private static final String KEY_MEDIA_ID = "KEY_MEDIA_ID";
    private static final String KEY_MEDIA_PATH = "KEY_MEDIA_PATH";
    private static final String KEY_MEDIA_NAME = "KEY_MEDIA_NAME";
    private static final String KEY_MEDIA_DURATION = "KEY_MEDIA_DURATION";
    private static final String KEY_MEDIA_BOOK_ID = "KEY_MEDIA_BOOK_ID";

    private static final String KEY_BOOK_ID = "KEY_BOOK_ID";
    private static final String KEY_BOOK_NAME = "KEY_BOOK_NAME";
    private static final String KEY_BOOK_COVER = "KEY_BOOK_COVER";
    private static final String KEY_BOOK_CURRENT_MEDIA_ID = "KEY_BOOK_CURRENT_MEDIA_ID";
    private static final String KEY_BOOK_CURRENT_MEDIA_POSITION = "KEY_BOOK_CURRENT_MEDIA_POSITION";
    private static final String KEY_BOOK_SORT_ID = "KEY_BOOK_SORT_ID";

    private static final String KEY_BOOKMARK_ID = "KEY_BOOKMARK_ID";
    private static final String KEY_BOOKMARK_POSITION = "KEY_BOOKMARK_POSITION";
    private static final String KEY_BOOKMARK_TITLE = "KEY_BOOKMARK_TITLE";

    private static DataBaseHelper instance;
    private final Context c;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null)
            instance = new DataBaseHelper(c);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + " ( " +
                    KEY_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_MEDIA_PATH + " TEXT NOT NULL, " +
                    KEY_MEDIA_NAME + " TEXT NOT NULL, " +
                    KEY_MEDIA_DURATION + " INTEGER NOT NULL, " +
                    KEY_MEDIA_BOOK_ID + " INTEGER NOT NULL)";
            db.execSQL(CREATE_MEDIA_TABLE);
            String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " ( " +
                    KEY_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_BOOK_NAME + " TEXT NOT NULL, " +
                    KEY_BOOK_COVER + " TEXT, " +
                    KEY_BOOK_CURRENT_MEDIA_ID + " INTEGER, " +
                    KEY_BOOK_CURRENT_MEDIA_POSITION + " INTEGER, " +
                    KEY_BOOK_SORT_ID + " INTEGER)";
            db.execSQL(CREATE_BOOK_TABLE);
            String CREATE_BOOKMARK_TABLE = "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
                    KEY_BOOKMARK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_BOOKMARK_TITLE + " TEXT NOT NULL, " +
                    KEY_BOOKMARK_POSITION + " INTEGER NOT NULL, " +
                    KEY_BOOK_ID + " INTEGER NOT NULL, " +
                    KEY_MEDIA_ID + " INTEGER NOT NULL)";
            db.execSQL(CREATE_BOOKMARK_TABLE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void upgradeThree(SQLiteDatabase db) {
        String CREATE_BOOKMARK_TABLE = "CREATE TABLE " + "TABLE_BOOKMARKS" + " ( " +
                "KEY_BOOKMARK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "KEY_BOOKMARK_TITLE" + " TEXT NOT NULL, " +
                "KEY_BOOKMARK_POSITION" + " INTEGER NOT NULL, " +
                "KEY_BOOK_ID" + " INTEGER NOT NULL, " +
                "KEY_MEDIA_ID" + " INTEGER NOT NULL)";

        db.execSQL(CREATE_BOOKMARK_TABLE);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        int integerBookId = sp.getInt("de.ph1b.audiobook.activity.BookChoose.SHARED_PREFS_CURRENT", -1);
        long longBookId = (long) integerBookId;
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("currentBook", longBookId);
        editor.apply();
    }

    private void upgradeOne(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String CREATE_MEDIA_TABLE = "CREATE TABLE " + "TABLE_MEDIA" + " ( " +
                    "KEY_MEDIA_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_MEDIA_PATH" + " TEXT, " +
                    "KEY_MEDIA_NAME" + " TEXT, " +
                    "KEY_MEDIA_POSITION" + " INTEGER, " +
                    "KEY_MEDIA_DURATION" + " INTEGER, " +
                    "KEY_MEDIA_BOOK_ID" + " INTEGER)";

            String CREATE_BOOK_TABLE = "CREATE TABLE " + "TABLE_BOOKS" + " ( " +
                    "KEY_BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_BOOK_NAME" + " TEXT, " +
                    "KEY_BOOK_COVER" + " TEXT, " +
                    "KEY_BOOK_THUMB" + " TEXT, " +
                    "KEY_BOOK_POSITION" + " INTEGER, " +
                    "KEY_BOOK_SORT_ID" + " INTEGER)";


            //first rename old tables
            db.execSQL("ALTER TABLE mediaTable RENAME TO tempMediaTable");
            db.execSQL("ALTER TABLE bookTable RENAME TO tempBookTable");

            //now create new tables
            db.execSQL(CREATE_MEDIA_TABLE);
            db.execSQL(CREATE_BOOK_TABLE);

            //now getting book table
            Cursor bookTableCursor = null;
            try {
                bookTableCursor = db.query("tempBookTable",
                        new String[]{"bookName", "bookCover", "bookMediaContaining", "bookPosition", "bookThumb"},
                        null, null, null, null, null);
                while (bookTableCursor.moveToNext()) {
                    String bookName = bookTableCursor.getString(0);
                    String bookCover = bookTableCursor.getString(1);
                    String bookMediaContaining = bookTableCursor.getString(2);
                    int bookPosition = bookTableCursor.getInt(3);
                    String bookThumb = bookTableCursor.getString(4);

                    //adding book in new table
                    ContentValues bookValues = new ContentValues();
                    bookValues.put("KEY_BOOK_NAME", bookName);
                    bookValues.put("KEY_BOOK_COVER", bookCover);
                    bookValues.put("KEY_BOOK_THUMB", bookThumb);
                    bookValues.put("KEY_BOOK_POSITION", bookPosition);
                    long newBookId = db.insert("TABLE_BOOKS", null, bookValues);
                    bookValues.put("KEY_BOOK_SORT_ID", newBookId);
                    db.update("TABLE_BOOKS", bookValues, "KEY_BOOK_ID" + " = " + newBookId, null);

                    //generate int array from string
                    String[] mediaIDsAsSplittedString = bookMediaContaining.split(",");
                    int[] mediaIDsAsSplittedInt = new int[mediaIDsAsSplittedString.length];
                    for (int i = 0; i < mediaIDsAsSplittedInt.length; i++) {
                        mediaIDsAsSplittedInt[i] = Integer.parseInt(mediaIDsAsSplittedString[i]);
                    }

                    for (int i : mediaIDsAsSplittedInt) {
                        Cursor mediaTableCursor = null;
                        try {
                            mediaTableCursor = db.query("tempMediaTable",
                                    new String[]{"mediaPath", "mediaName", "mediaPosition", "mediaDuration"},
                                    "mediaID = " + i, null, null, null, null);
                            if (mediaTableCursor.moveToFirst()) {
                                String mediaPath = mediaTableCursor.getString(0);
                                String mediaName = mediaTableCursor.getString(1);
                                int mediaPosition = mediaTableCursor.getInt(2);
                                int mediaDuration = mediaTableCursor.getInt(3);

                                //adding these values in new media table
                                ContentValues mediaValues = new ContentValues();
                                mediaValues.put("KEY_MEDIA_PATH", mediaPath);
                                mediaValues.put("KEY_MEDIA_NAME", mediaName);
                                mediaValues.put("KEY_MEDIA_POSITION", mediaPosition);
                                mediaValues.put("KEY_MEDIA_DURATION", mediaDuration);
                                mediaValues.put("KEY_MEDIA_BOOK_ID", newBookId);
                                db.insert("TABLE_MEDIA", null, mediaValues);
                            }
                        } finally {
                            if (mediaTableCursor != null) {
                                mediaTableCursor.close();
                            }
                        }
                    }

                }

            } finally {
                if (bookTableCursor != null) {
                    bookTableCursor.close();
                }
            }

            //dropping temporary tables
            db.execSQL("DROP TABLE tempMediaTable");
            db.execSQL("DROP TABLE tempBookTable");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void upgradeTwo(SQLiteDatabase db) {
        // first rename old tables
        db.execSQL("ALTER TABLE TABLE_MEDIA RENAME TO TEMP_TABLE_MEDIA");
        db.execSQL("ALTER TABLE TABLE_BOOKS RENAME TO TEMP_TABLE_BOOKS");

        String CREATE_MEDIA_TABLE = "CREATE TABLE " + "TABLE_MEDIA" + " ( " +
                "KEY_MEDIA_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "KEY_MEDIA_PATH" + " TEXT, " +
                "KEY_MEDIA_NAME" + " TEXT, " +
                "KEY_MEDIA_DURATION" + " INTEGER, " +
                "KEY_MEDIA_BOOK_ID" + " INTEGER)";

        String CREATE_BOOK_TABLE = "CREATE TABLE " + "TABLE_BOOKS" + " ( " +
                "KEY_BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "KEY_BOOK_NAME" + " TEXT, " +
                "KEY_BOOK_COVER" + " TEXT, " +
                "KEY_BOOK_CURRENT_MEDIA_ID" + " INTEGER, " +
                "KEY_BOOK_CURRENT_MEDIA_POSITION" + " INTEGER, " +
                "KEY_BOOK_SORT_ID" + " INTEGER)";

        // new create new tables
        db.execSQL(CREATE_MEDIA_TABLE);
        db.execSQL(CREATE_BOOK_TABLE);

        // getting data from old table
        Cursor bookTableCursor = null;
        try {
            bookTableCursor = db.query("TEMP_TABLE_BOOKS",
                    new String[]{"KEY_BOOK_ID", "KEY_BOOK_NAME", "KEY_BOOK_COVER", "KEY_BOOK_POSITION", "KEY_BOOK_SORT_ID", "KEY_BOOK_THUMB"},
                    null, null, null, null, null);
            //going through all books and updating them with current values
            while (bookTableCursor.moveToNext()) {
                int oldBookId = bookTableCursor.getInt(0);
                String bookName = bookTableCursor.getString(1);
                String bookCover = bookTableCursor.getString(2);
                int bookPosition = bookTableCursor.getInt(3);
                int bookSortId = bookTableCursor.getInt(4);
                String bookThumb = bookTableCursor.getString(5);

                //deleting unnecessary thumbs
                if (bookThumb != null) {
                    File f = new File(bookThumb);
                    if (f.isFile()) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }

                //converts png cover to jpg and deletes old cover
                if (bookCover != null) {
                    File f = new File(bookCover);
                    if (f.exists()) {
                        if (f.getName().toLowerCase().endsWith(".png")) {
                            if (f.exists()) {
                                bookCover = ImageHelper.saveCover(BitmapFactory.decodeFile(f.getAbsolutePath()), c);
                                //noinspection ResultOfMethodCallIgnored
                                f.delete();
                            }
                        }
                    }
                }

                Cursor mediaPositionCursor = null;
                try {
                    mediaPositionCursor = db.query("TEMP_TABLE_MEDIA",
                            new String[]{"KEY_MEDIA_POSITION"},
                            "KEY_MEDIA_BOOK_ID = " + oldBookId + " AND KEY_MEDIA_ID = " + bookPosition,
                            null, null, null, null, null);
                    int bookMediaPosition = 0;
                    if (mediaPositionCursor.moveToFirst())
                        bookMediaPosition = mediaPositionCursor.getInt(0);
                    ContentValues bookValues = new ContentValues();
                    bookValues.put("KEY_BOOK_NAME", bookName);
                    bookValues.put("KEY_BOOK_COVER", bookCover);
                    bookValues.put("KEY_BOOK_CURRENT_MEDIA_ID", bookPosition);
                    bookValues.put("KEY_BOOK_CURRENT_MEDIA_POSITION", bookMediaPosition);
                    bookValues.put("KEY_BOOK_SORT_ID", bookSortId);

                    int newBookId = (int) db.insert("TABLE_BOOKS", null, bookValues);

                    Cursor mediaTableCursor = null;
                    try {
                        mediaTableCursor = db.query("TEMP_TABLE_MEDIA",
                                new String[]{"KEY_MEDIA_PATH", "KEY_MEDIA_NAME", "KEY_MEDIA_DURATION"},
                                "KEY_MEDIA_BOOK_ID = " + oldBookId,
                                null, null, null, null);
                        while (mediaTableCursor.moveToNext()) {
                            String mediaPath = mediaTableCursor.getString(0);
                            String mediaName = mediaTableCursor.getString(1);
                            int mediaDuration = mediaTableCursor.getInt(2);
                            ContentValues mediaValues = new ContentValues();
                            mediaValues.put("KEY_MEDIA_PATH", mediaPath);
                            mediaValues.put("KEY_MEDIA_NAME", mediaName);
                            mediaValues.put("KEY_MEDIA_DURATION", mediaDuration);
                            mediaValues.put("KEY_MEDIA_BOOK_ID", newBookId);
                            db.insert("TABLE_MEDIA", null, mediaValues);
                        }
                    } finally {
                        if (mediaTableCursor != null) {
                            mediaTableCursor.close();
                        }
                    }
                } finally {
                    if (mediaPositionCursor != null) {
                        mediaPositionCursor.close();
                    }
                }
            }
        } finally {
            if (bookTableCursor != null) {
                bookTableCursor.close();
            }
        }

        // dropping old table
        db.execSQL("DROP TABLE TEMP_TABLE_MEDIA");
        db.execSQL("DROP TABLE TEMP_TABLE_BOOKS");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        while (oldVersion < newVersion) {
            if (BuildConfig.DEBUG)
                Log.d("dbh", "upgrading version" + oldVersion);
            switch (oldVersion) {
                case 1:
                    upgradeOne(db);
                    break;
                case 2:
                    upgradeTwo(db);
                    break;
                case 3:
                    upgradeThree(db);
                    break;
            }
            oldVersion++;
        }
    }


    public BookDetail getBook(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKS,
                    null,
                    KEY_BOOK_ID + " = " + id, null, null, null, null);
            if (cursor.moveToFirst()) {
                BookDetail book = new BookDetail();
                book.setId(cursor.getLong(0));
                book.setName(cursor.getString(1));
                book.setCover(cursor.getString(2));
                book.setCurrentMediaId(cursor.getInt(3));
                book.setCurrentMediaPosition(cursor.getInt(4));
                book.setSortId(cursor.getInt(5));

                return book;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    public void addMedia(ArrayList<MediaDetail> media) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (MediaDetail m : media) {
                ContentValues values = new ContentValues();
                values.put(KEY_MEDIA_PATH, m.getPath()); // get title
                values.put(KEY_MEDIA_NAME, m.getName());
                values.put(KEY_MEDIA_DURATION, m.getDuration());
                values.put(KEY_MEDIA_BOOK_ID, m.getBookId());
                db.insert(TABLE_MEDIA, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public long addBookmark(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_BOOKMARK_TITLE, bookmark.getTitle());
        cv.put(KEY_BOOKMARK_POSITION, bookmark.getPosition());
        cv.put(KEY_BOOK_ID, bookmark.getBookId());
        cv.put(KEY_MEDIA_ID, bookmark.getMediaId());
        SQLiteDatabase db = getWritableDatabase();
        return db.insertOrThrow(TABLE_BOOKMARKS, null, cv);
    }

    public ArrayList<Bookmark> getAllBookmarks(long bookId) {
        ArrayList<Bookmark> allBookmarks = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS,
                new String[]{KEY_BOOKMARK_TITLE, KEY_BOOKMARK_POSITION, KEY_MEDIA_ID},
                KEY_BOOK_ID + "=?",
                new String[]{String.valueOf(bookId)},
                null, null, KEY_MEDIA_ID);
        try {
            while (cursor.moveToNext()) {
                Bookmark bookmark = new Bookmark();
                String title = cursor.getString(0);
                int position = cursor.getInt(1);
                long mediaId = cursor.getLong(2);

                bookmark.setTitle(title);
                bookmark.setPosition(position);
                bookmark.setBookId(bookId);
                bookmark.setMediaId(mediaId);
                allBookmarks.add(bookmark);
            }
        } finally {
            cursor.close();
        }
        return allBookmarks;
    }

    public void updateBookmark(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_BOOKMARK_TITLE, bookmark.getTitle());
        cv.put(KEY_BOOKMARK_POSITION, bookmark.getPosition());
        cv.put(KEY_BOOK_ID, bookmark.getBookId());
        cv.put(KEY_MEDIA_ID, bookmark.getMediaId());
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_BOOKMARKS, cv, KEY_BOOKMARK_ID + "=?", new String[]{String.valueOf(bookmark.getId())});
    }

    public void deleteBookmark(Bookmark bookmark) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, bookmark.getId() + "=?", new String[]{String.valueOf(bookmark.getId())});
    }


    public int addBook(BookDetail book) {
        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int bookId = 0;
        try {
            bookId = (int) db.insert(TABLE_BOOKS, null, values);
            values.put(KEY_BOOK_SORT_ID, bookId);
            db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + bookId, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return bookId;
    }


    public ArrayList<MediaDetail> getMediaFromBook(long bookId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_MEDIA,
                    null,
                    KEY_MEDIA_BOOK_ID + " = " + bookId,
                    null, null, null,
                    KEY_MEDIA_ID);
            ArrayList<MediaDetail> containingMedia = new ArrayList<>();
            while (cursor.moveToNext()) {
                MediaDetail media = new MediaDetail();
                media.setId(cursor.getLong(0));
                media.setPath(cursor.getString(1));
                media.setName(cursor.getString(2));
                media.setDuration(cursor.getInt(3));
                media.setBookId(cursor.getLong(4));
                containingMedia.add(media);
            }
            return containingMedia;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    public ArrayList<BookDetail> getAllBooks() {
        ArrayList<BookDetail> allBooks = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKS,
                    null, null, null, null, null,
                    KEY_BOOK_SORT_ID);
            while (cursor.moveToNext()) {
                BookDetail book = new BookDetail();
                book.setId(cursor.getLong(0));
                book.setName(cursor.getString(1));
                book.setCover(cursor.getString(2));
                book.setCurrentMediaId(cursor.getInt(3));
                book.setCurrentMediaPosition(cursor.getInt(4));
                book.setSortId(cursor.getInt(5));
                allBooks.add(book);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return allBooks;
    }


    public void updateBook(BookDetail book) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_CURRENT_MEDIA_ID, book.getCurrentMediaId());
        values.put(KEY_BOOK_CURRENT_MEDIA_POSITION, book.getCurrentMediaPosition());
        values.put(KEY_BOOK_SORT_ID, book.getSortId());

        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + book.getId(), null);
    }


    public void deleteBook(BookDetail book) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long bookId = book.getId();
            db.delete(TABLE_MEDIA,
                    KEY_MEDIA_BOOK_ID + "=?",
                    new String[]{String.valueOf(bookId)});
            db.delete(TABLE_BOOKS,
                    KEY_BOOK_ID + "=?",
                    new String[]{String.valueOf(bookId)});
            db.delete(TABLE_BOOKMARKS,
                    KEY_BOOK_ID + "=?",
                    new String[]{String.valueOf(bookId)});
            String cover = book.getCover();
            if (cover != null) {
                File f = new File(cover);
                if (f.exists() && f.canWrite()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}