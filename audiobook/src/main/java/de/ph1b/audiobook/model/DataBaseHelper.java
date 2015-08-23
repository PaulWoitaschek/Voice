package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;

import static com.google.common.base.Preconditions.checkArgument;

@ThreadSafe
@SuppressWarnings("TryFinallyCanBeTryWithResources")
/**
 * This is the helper for the apps database. All Database writing is done by an executor and the
 * helper holds a internal array of the books.
 */
public class DataBaseHelper extends SQLiteOpenHelper {

    // book keys
    public static final String BOOK_ID = "bookId";
    public static final String BOOK_NAME = "bookName";
    public static final String BOOK_AUTHOR = "bookAuthor";
    public static final String BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath";
    public static final String BOOK_PLAYBACK_SPEED = "bookSpeed";
    public static final String BOOK_ROOT = "bookRoot";
    public static final String BOOK_TIME = "bookTime";
    public static final String BOOK_TYPE = "bookType";
    public static final String BOOK_USE_COVER_REPLACEMENT = "bookUseCoverReplacement";
    public static final String BOOK_ACTIVE = "BOOK_ACTIVE";
    public static final String CHAPTER_DURATION = "chapterDuration";
    public static final String CHAPTER_NAME = "chapterName";
    public static final String CHAPTER_PATH = "chapterPath";
    public static final String BOOKMARK_TIME = "bookmarkTime";
    public static final String BOOKMARK_PATH = "bookmarkPath";
    public static final String BOOKMARK_TITLE = "bookmarkTitle";
    private static final int DATABASE_VERSION = 32;
    private static final String DATABASE_NAME = "autoBookDB";
    private static final String TABLE_BOOK = "tableBooks";
    private static final String TABLE_CHAPTERS = "tableChapters";
    private static final String TABLE_BOOKMARKS = "tableBookmarks";
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
    private static final String CREATE_TABLE_CHAPTERS = "CREATE TABLE " + TABLE_CHAPTERS + " ( " +
            CHAPTER_DURATION + " INTEGER NOT NULL, " +
            CHAPTER_NAME + " TEXT NOT NULL, " +
            CHAPTER_PATH + " TEXT NOT NULL, " +
            BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + TABLE_BOOK + "(" + BOOK_ID + "))";
    private static final String CREATE_TABLE_BOOKMARKS = "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
            BOOKMARK_PATH + " TEXT NOT NULL, " +
            BOOKMARK_TITLE + " TEXT NOT NULL, " +
            BOOKMARK_TIME + " INTEGER NOT NULL, " +
            BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + TABLE_BOOK + "(" + BOOK_ID + "))";
    private static final String TAG = DataBaseHelper.class.getSimpleName();
    private static final Communication COMMUNICATION = Communication.getInstance();
    private static DataBaseHelper instance;
    private final Context c;
    private final List<Book> activeBooks;
    private final List<Book> orphanedBooks;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;

        SQLiteDatabase db = getReadableDatabase();
        Cursor bookCursor = db.query(TABLE_BOOK,
                new String[]{BOOK_ID, BOOK_NAME, BOOK_AUTHOR, BOOK_CURRENT_MEDIA_PATH,
                        BOOK_PLAYBACK_SPEED, BOOK_ROOT, BOOK_TIME, BOOK_TYPE, BOOK_USE_COVER_REPLACEMENT,
                        BOOK_ACTIVE},
                null, null, null, null, null);
        try {
            activeBooks = new ArrayList<>(bookCursor.getCount());
            orphanedBooks = new ArrayList<>(bookCursor.getCount());
            while (bookCursor.moveToNext()) {
                long bookId = bookCursor.getLong(0);
                String bookName = bookCursor.getString(1);
                String bookAuthor = bookCursor.getString(2);
                File bookmarkCurrentMediaPath = new File(bookCursor.getString(3));
                float bookSpeed = bookCursor.getFloat(4);
                String bookRoot = bookCursor.getString(5);
                int bookTime = bookCursor.getInt(6);
                Book.Type bookType = Book.Type.valueOf(bookCursor.getString(7));
                boolean bookUseCoverReplacement = bookCursor.getInt(8) == 1;
                boolean bookActive = bookCursor.getInt(9) == 1;

                Cursor chapterCursor = db.query(TABLE_CHAPTERS,
                        new String[]{CHAPTER_DURATION, CHAPTER_NAME, CHAPTER_PATH},
                        BOOK_ID + "=?",
                        new String[]{String.valueOf(bookId)},
                        null, null, null);
                List<Chapter> chapters = new ArrayList<>(chapterCursor.getCount());
                try {
                    while (chapterCursor.moveToNext()) {
                        int chapterDuration = chapterCursor.getInt(0);
                        String chapterName = chapterCursor.getString(1);
                        File chapterFile = new File(chapterCursor.getString(2));
                        chapters.add(new Chapter(chapterFile, chapterName, chapterDuration));
                    }
                } finally {
                    chapterCursor.close();
                }
                Collections.sort(chapters);

                Cursor bookmarkCursor = db.query(TABLE_BOOKMARKS,
                        new String[]{BOOKMARK_PATH, BOOKMARK_TIME, BOOKMARK_TITLE},
                        BOOK_ID + "=?", new String[]{String.valueOf(bookId)}
                        , null, null, null);
                List<Bookmark> bookmarks = new ArrayList<>(bookmarkCursor.getCount());
                try {
                    while (bookmarkCursor.moveToNext()) {
                        File bookmarkFile = new File(bookmarkCursor.getString(0));
                        int bookmarkTime = bookmarkCursor.getInt(1);
                        String bookmarkTitle = bookmarkCursor.getString(2);
                        bookmarks.add(new Bookmark(bookmarkFile, bookmarkTitle, bookmarkTime));
                    }
                } finally {
                    bookmarkCursor.close();
                }

                Book book = new Book(bookRoot, bookName, bookAuthor, chapters,
                        bookmarkCurrentMediaPath, bookType, bookmarks, c);
                book.setPlaybackSpeed(bookSpeed);
                book.setPosition(bookTime, bookmarkCurrentMediaPath);
                book.setUseCoverReplacement(bookUseCoverReplacement);
                book.setId(bookId);

                if (bookActive) {
                    activeBooks.add(book);
                } else {
                    orphanedBooks.add(book);
                }
            }
        } finally {
            bookCursor.close();
        }
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DataBaseHelper(c.getApplicationContext());
        }
        return instance;
    }

    public synchronized void addBook(@NonNull final Book mutableBook) {
        L.v(TAG, "addBook=" + mutableBook.getName());
        checkArgument(!mutableBook.getChapters().isEmpty());

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues bookCv = mutableBook.getContentValues();

            long bookId = db.insert(TABLE_BOOK, null, bookCv);
            mutableBook.setId(bookId);

            for (Chapter c : mutableBook.getChapters()) {
                ContentValues chapterCv = c.getContentValues(mutableBook.getId());
                db.insert(TABLE_CHAPTERS, null, chapterCv);
            }

            for (Bookmark b : mutableBook.getBookmarks()) {
                ContentValues bookmarkCv = b.getContentValues(mutableBook.getId());
                db.insert(TABLE_BOOKMARKS, null, bookmarkCv);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        activeBooks.add(new Book(mutableBook));
        COMMUNICATION.bookSetChanged(activeBooks);
    }

    @Nullable
    public synchronized Book getBook(long id) {
        for (Book b : activeBooks) {
            if (b.getId() == id) {
                return new Book(b);
            }
        }
        return null;
    }


    @NonNull
    public synchronized List<Book> getActiveBooks() {
        List<Book> copyBooks = new ArrayList<>(activeBooks.size());
        for (Book b : activeBooks) {
            copyBooks.add(new Book(b));
        }
        return copyBooks;
    }

    @NonNull
    public synchronized List<Book> getOrphanedBooks() {
        List<Book> copyBooks = new ArrayList<>(orphanedBooks.size());
        for (Book b : orphanedBooks) {
            copyBooks.add(new Book(b));
        }
        return copyBooks;
    }

    public synchronized void updateBook(@NonNull Book mutableBook) {
        L.v(TAG, "updateBook=" + mutableBook.getName());
        checkArgument(!mutableBook.getChapters().isEmpty());

        ListIterator<Book> bookIterator = activeBooks.listIterator();
        while (bookIterator.hasNext()) {
            Book next = bookIterator.next();
            if (mutableBook.getId() == next.getId()) {
                bookIterator.set(new Book(mutableBook));

                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    // update book itself
                    ContentValues bookCv = mutableBook.getContentValues();
                    db.update(TABLE_BOOK, bookCv, BOOK_ID + "=?", new String[]{String.valueOf(mutableBook.getId())});

                    // delete old chapters and replace them with new ones
                    db.delete(TABLE_CHAPTERS, BOOK_ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                    for (Chapter c : mutableBook.getChapters()) {
                        ContentValues chapterCv = c.getContentValues(mutableBook.getId());
                        db.insert(TABLE_CHAPTERS, null, chapterCv);
                    }

                    // replace old bookmarks and replace them with new ones
                    db.delete(TABLE_BOOKMARKS, BOOK_ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                    for (Bookmark b : mutableBook.getBookmarks()) {
                        ContentValues bookmarkCV = b.getContentValues(mutableBook.getId());
                        db.insert(TABLE_BOOKMARKS, null, bookmarkCV);
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                break;
            }
        }

        COMMUNICATION.sendBookContentChanged(mutableBook);
    }

    public synchronized void hideBook(@NonNull Book mutableBook) {
        L.v(TAG, "hideBook=" + mutableBook.getName());
        checkArgument(!mutableBook.getChapters().isEmpty());

        ListIterator<Book> iterator = activeBooks.listIterator();
        while (iterator.hasNext()) {
            Book next = iterator.next();
            if (next.getId() == mutableBook.getId()) {
                iterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BOOK_ACTIVE, 0);
                getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                break;
            }
        }
        orphanedBooks.add(new Book(mutableBook));
        COMMUNICATION.bookSetChanged(activeBooks);
    }

    public synchronized void revealBook(@NonNull Book mutableBook) {
        checkArgument(!mutableBook.getChapters().isEmpty());

        Iterator<Book> orphanedBookIterator = orphanedBooks.iterator();
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().getId() == mutableBook.getId()) {
                orphanedBookIterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BOOK_ACTIVE, 1);
                getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                break;
            }
        }
        activeBooks.add(new Book(mutableBook));
        COMMUNICATION.bookSetChanged(activeBooks);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOK);
        db.execSQL(CREATE_TABLE_CHAPTERS);
        db.execSQL(CREATE_TABLE_BOOKMARKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            DataBaseUpgradeHelper upgradeHelper = new DataBaseUpgradeHelper(db, c);
            upgradeHelper.upgrade(oldVersion);
        } catch (InvalidPropertiesFormatException e) {
            L.e(TAG, "Error at upgrade", e);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
            onCreate(db);
        }
    }
}
