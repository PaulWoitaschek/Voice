package de.ph1b.audiobook.content;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Prefs;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DataBaseHelper";

    private static final String DATABASE_NAME = "audioBookDB";
    private static final int DATABASE_VERSION = 5;

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
    private static final String KEY_BOOK_POSITION = "KEY_BOOK_POSITION";
    private static final String KEY_BOOK_TIME = "KEY_BOOK_TIME";
    private static final String KEY_BOOK_SORT_ID = "KEY_BOOK_SORT_ID";

    private static final String KEY_BOOKMARK_ID = "KEY_BOOKMARK_ID";
    private static final String KEY_BOOKMARK_POSITION = "KEY_BOOKMARK_POSITION";
    private static final String KEY_BOOKMARK_TITLE = "KEY_BOOKMARK_TITLE";
    private static final String KEY_BOOKMARK_TIME = "KEY_BOOKMARK_TIME";

    private static DataBaseHelper instance;
    private final Context c;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null)
            instance = new DataBaseHelper(c.getApplicationContext());
        return instance;
    }

    public static void setMissingDuration(Media m, MediaMetadataRetriever metaRetriever) {
        try {
            if (m.getDuration() != 0) {
                metaRetriever.setDataSource(m.getPath());
                int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                m.setDuration(duration);
            }
        } catch (RuntimeException e) { //undocumented exception
            L.e(TAG, "Error at retrieving duration from file=" + m.getPath(), e);
        }
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
                    KEY_BOOK_POSITION + " INTEGER, " +
                    KEY_BOOK_TIME + " INTEGER, " +
                    KEY_BOOK_SORT_ID + " INTEGER)";
            db.execSQL(CREATE_BOOK_TABLE);
            String CREATE_BOOKMARK_TABLE = "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
                    KEY_BOOKMARK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_BOOKMARK_TITLE + " TEXT NOT NULL, " +
                    KEY_BOOKMARK_POSITION + " INTEGER NOT NULL, " +
                    KEY_BOOK_ID + " INTEGER NOT NULL, " +
                    KEY_BOOKMARK_TIME + " INTEGER NOT NULL)";
            db.execSQL(CREATE_BOOKMARK_TABLE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void upgradeThree(SQLiteDatabase db) {
        Prefs prefs = new Prefs(c);
        long oldCurrentBookId = prefs.getCurrentBookId();

        db.beginTransaction();
        try {
            //bookmarks
            String CREATE_BOOKMARK_TABLE = "CREATE TABLE " + "TABLE_BOOKMARKS" + " ( " +
                    "KEY_BOOKMARK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_BOOKMARK_TITLE" + " TEXT NOT NULL, " +
                    "KEY_BOOKMARK_POSITION" + " INTEGER NOT NULL, " +
                    "KEY_BOOK_ID" + " INTEGER NOT NULL, " +
                    "KEY_BOOKMARK_TIME" + " INTEGER NOT NULL)";
            db.execSQL(CREATE_BOOKMARK_TABLE);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
            int integerBookId = sp.getInt("de.ph1b.audiobook.activity.BookChoose.SHARED_PREFS_CURRENT", -1);
            long longBookId = (long) integerBookId;
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("currentBook", longBookId);
            editor.apply();

            //updating tables to represent a position instead the id.
            db.execSQL("ALTER TABLE TABLE_BOOKS RENAME TO TABLE_BOOKS_TEMP");
            String CREATE_BOOK_TABLE = "CREATE TABLE " + "TABLE_BOOKS" + " ( " +
                    "KEY_BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_BOOK_NAME" + " TEXT NOT NULL, " +
                    "KEY_BOOK_COVER" + " TEXT, " +
                    "KEY_BOOK_POSITION" + " INTEGER, " +
                    "KEY_BOOK_TIME" + " INTEGER, " +
                    "KEY_BOOK_SORT_ID" + " INTEGER)";
            db.execSQL(CREATE_BOOK_TABLE);

            Cursor books = db.query("TABLE_BOOKS_TEMP",
                    new String[]{"KEY_BOOK_ID", "KEY_BOOK_NAME", "KEY_BOOK_COVER",
                            "KEY_BOOK_CURRENT_MEDIA_ID", "KEY_BOOK_CURRENT_MEDIA_POSITION"},
                    null, null, null, null, null);
            try {
                while (books.moveToNext()) {
                    long oldBookId = books.getLong(0);
                    String bookName = books.getString(1);
                    String bookCover = books.getString(2);
                    long currentMediaId = books.getLong(3);
                    int currentMediaPosition = books.getInt(4);
                    Cursor media = db.query("TABLE_MEDIA", //table
                            new String[]{"KEY_MEDIA_ID"}, //query
                            "KEY_MEDIA_BOOK_ID" + "=?", //selection
                            new String[]{String.valueOf(oldBookId)}, //selection args
                            null, null,
                            "KEY_MEDIA_ID"); //sort by
                    try {
                        int position = -1; //get position in the future Media Array
                        while (media.moveToNext()) {
                            position++;
                            if (media.getLong(0) == currentMediaId) {
                                break;
                            }
                        }
                        ContentValues cv = new ContentValues();
                        cv.put("KEY_BOOK_NAME", bookName);
                        cv.put("KEY_BOOK_COVER", bookCover);
                        cv.put("KEY_BOOK_POSITION", position);
                        cv.put("KEY_BOOK_TIME", currentMediaPosition);
                        cv.put("KEY_BOOK_SORT_ID", 0);
                        long newBookId = db.insert("TABLE_BOOKS", null, cv);
                        cv.put("KEY_BOOK_SORT_ID", newBookId);
                        db.update("TABLE_BOOKS", cv, "KEY_BOOK_ID" + "=?", new String[]{String.valueOf(newBookId)});

                        // updating current book
                        if (oldBookId == oldCurrentBookId) {
                            prefs.setCurrentBookId(newBookId);
                        }
                    } finally {
                        media.close();
                    }
                }
            } finally {
                books.close();
            }
            db.execSQL("DROP TABLE TABLE_BOOKS_TEMP");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
            Cursor bookTableCursor = db.query("tempBookTable",
                    new String[]{"bookName", "bookCover", "bookMediaContaining", "bookPosition", "bookThumb"},
                    null, null, null, null, null);
            try {
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
                    db.update("TABLE_BOOKS", bookValues, "KEY_BOOK_ID" + "=?",
                            new String[]{String.valueOf(newBookId)});

                    //generate int array from string
                    String[] mediaIDsAsSplittedString = bookMediaContaining.split(",");
                    int[] mediaIDsAsSplittedInt = new int[mediaIDsAsSplittedString.length];
                    for (int i = 0; i < mediaIDsAsSplittedInt.length; i++) {
                        mediaIDsAsSplittedInt[i] = Integer.parseInt(mediaIDsAsSplittedString[i]);
                    }

                    for (int i : mediaIDsAsSplittedInt) {
                        Cursor mediaTableCursor = db.query("tempMediaTable",
                                new String[]{"mediaPath", "mediaName", "mediaPosition", "mediaDuration"},
                                "mediaID = " + i, null, null, null, null);
                        try {
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
                            mediaTableCursor.close();
                        }
                    }

                }

            } finally {
                bookTableCursor.close();
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
        Cursor bookTableCursor = db.query("TEMP_TABLE_BOOKS",
                new String[]{"KEY_BOOK_ID", "KEY_BOOK_NAME", "KEY_BOOK_COVER", "KEY_BOOK_POSITION", "KEY_BOOK_SORT_ID", "KEY_BOOK_THUMB"},
                null, null, null, null, null);
        try {
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

                Cursor mediaPositionCursor = db.query("TEMP_TABLE_MEDIA",
                        new String[]{"KEY_MEDIA_POSITION"},
                        "KEY_MEDIA_BOOK_ID = " + oldBookId + " AND KEY_MEDIA_ID = " + bookPosition,
                        null, null, null, null, null);
                try {
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

                    Cursor mediaTableCursor = db.query("TEMP_TABLE_MEDIA",
                            new String[]{"KEY_MEDIA_PATH", "KEY_MEDIA_NAME", "KEY_MEDIA_DURATION"},
                            "KEY_MEDIA_BOOK_ID = " + oldBookId,
                            null, null, null, null);
                    try {
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
                        mediaTableCursor.close();
                    }
                } finally {
                    mediaPositionCursor.close();
                }
            }
        } finally {
            bookTableCursor.close();
        }

        // dropping old table
        db.execSQL("DROP TABLE TEMP_TABLE_MEDIA");
        db.execSQL("DROP TABLE TEMP_TABLE_BOOKS");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        while (oldVersion < newVersion) {
            L.d("dbh", "upgrading version" + oldVersion);
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

    public ArrayList<Book> getAllBooks() {
        ArrayList<Book> allBooks = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.query(TABLE_BOOKS,
                    new String[]{KEY_BOOK_ID}, null, null, null, null,
                    KEY_BOOK_SORT_ID);
            try {
                while (cursor.moveToNext()) {
                    long bookId = cursor.getLong(0);
                    Book book = getBook(bookId, db);
                    allBooks.add(book);
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }


        return allBooks;
    }

    public void setMissingDurations(ArrayList<Media> medias) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        try {
            for (Media m : medias) {
                setMissingDuration(m, metaRetriever);
            }
        } finally {
            metaRetriever.release();
        }

        ArrayList<ContentValues> cvs = new ArrayList<>();
        for (Media m : medias) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_MEDIA_DURATION, m.getDuration());
        }

        // updating these media in database
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Media m : medias) {
                ContentValues cv = new ContentValues();
                cv.put(KEY_MEDIA_DURATION, m.getDuration());
                db.update(TABLE_MEDIA, cv, KEY_MEDIA_ID + "=?", new String[]{String.valueOf(m.getId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Book getBook(long bookId, SQLiteDatabase db) {
        Cursor bookCursor = db.query(TABLE_BOOKS,
                new String[]{KEY_BOOK_NAME, KEY_BOOK_COVER, KEY_BOOK_POSITION, KEY_BOOK_TIME, KEY_BOOK_SORT_ID},
                KEY_BOOK_ID + "=?", new String[]{String.valueOf(bookId)}, null, null, null);
        try {
            if (bookCursor.moveToFirst()) {

                String bookName = bookCursor.getString(0);
                String cover = bookCursor.getString(1);
                int position = bookCursor.getInt(2);
                int time = bookCursor.getInt(3);
                long sortId = bookCursor.getLong(4);

                Book book = new Book();
                book.setName(bookName);
                book.setCover(cover);
                book.setPosition(position);
                book.setTime(time);
                book.setSortId(sortId);
                book.setId(bookId);

                ArrayList<Media> containingMedia = new ArrayList<>();

                Cursor mediaCursor = db.query(TABLE_MEDIA,
                        new String[]{KEY_MEDIA_ID, KEY_MEDIA_PATH, KEY_MEDIA_NAME, KEY_MEDIA_DURATION},
                        KEY_MEDIA_BOOK_ID + "=?",
                        new String[]{String.valueOf(bookId)},
                        null, null,
                        KEY_MEDIA_ID);
                try {
                    while (mediaCursor.moveToNext()) {
                        long id = mediaCursor.getLong(0);
                        String path = mediaCursor.getString(1);
                        String mediaName = mediaCursor.getString(2);
                        int duration = mediaCursor.getInt(3);
                        Media media = new Media(path, mediaName, bookId);
                        media.setId(id);
                        media.setDuration(duration);
                        containingMedia.add(media);
                    }
                    L.d(TAG, "getMediaFromBook returned size: " + containingMedia.size());
                } finally {
                    mediaCursor.close();
                }

                book.setContainingMedia(containingMedia);
                L.d(TAG, "got book with containing media size: " + book.getContainingMedia());
                L.d(TAG, "so book is:" + book);
                return book;
            }
        } finally {
            bookCursor.close();
        }
        return null;
    }


    public Book getBook(long id) {
        return getBook(id, getReadableDatabase());
    }


    public void addMedia(ArrayList<Media> media) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Media m : media) {
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
        cv.put(KEY_BOOKMARK_TIME, bookmark.getTime());
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_BOOKMARKS, null, cv);
    }

    public ArrayList<Bookmark> getAllBookmarks(long bookId) {
        ArrayList<Bookmark> allBookmarks = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS,
                new String[]{KEY_BOOKMARK_TITLE, KEY_BOOKMARK_POSITION, KEY_BOOKMARK_TIME, KEY_BOOKMARK_ID},
                KEY_BOOK_ID + "=?",
                new String[]{String.valueOf(bookId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                int position = cursor.getInt(1);
                int time = cursor.getInt(2);
                long bookmarkId = cursor.getLong(3);

                Bookmark bookmark = new Bookmark(bookId, position, time);
                bookmark.setTitle(title);
                bookmark.setId(bookmarkId);
                allBookmarks.add(bookmark);
            }
        } finally {
            cursor.close();
        }
        Collections.sort(allBookmarks);
        return allBookmarks;
    }

    public void updateBookmark(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_BOOKMARK_TITLE, bookmark.getTitle());
        cv.put(KEY_BOOKMARK_POSITION, bookmark.getPosition());
        cv.put(KEY_BOOK_ID, bookmark.getBookId());
        cv.put(KEY_BOOKMARK_TIME, bookmark.getTime());
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_BOOKMARKS, cv, KEY_BOOKMARK_ID + "=?", new String[]{String.valueOf(bookmark.getId())});
    }

    public void deleteBookmark(Bookmark bookmark) {
        L.d(TAG, "Deleting bookmark from database: " + bookmark.getId());
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_BOOKMARKS, KEY_BOOKMARK_ID + "=?", new String[]{String.valueOf(bookmark.getId())});
        L.d(TAG, "deleted n bookmarks: " + result);
    }


    public long addBook(Book book) {
        L.d(TAG, "addBook called");
        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long bookId = 0;
        try {
            bookId = db.insert(TABLE_BOOKS, null, values);
            values.put(KEY_BOOK_SORT_ID, bookId);
            db.update(TABLE_BOOKS, values, KEY_BOOK_ID + "=?", new String[]{String.valueOf(bookId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return bookId;
    }


    public void updateBook(Book book) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_TIME, book.getTime());
        values.put(KEY_BOOK_POSITION, book.getPosition());
        values.put(KEY_BOOK_SORT_ID, book.getSortId());

        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
    }


    public void deleteBook(Book book) {
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