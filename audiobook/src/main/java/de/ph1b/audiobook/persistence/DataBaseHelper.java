package de.ph1b.audiobook.persistence;

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

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * This is the helper for the apps database. All Database writing is done by an executor and the
 * helper holds a internal array of the books.
 */
@ThreadSafe
@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 32;
    private static final String DATABASE_NAME = "autoBookDB";
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
        Cursor bookCursor = db.query(BookTable.TABLE_NAME,
                new String[]{BookTable.ID, BookTable.NAME, BookTable.AUTHOR, BookTable.CURRENT_MEDIA_PATH,
                        BookTable.PLAYBACK_SPEED, BookTable.ROOT, BookTable.TIME, BookTable.TYPE, BookTable.USE_COVER_REPLACEMENT,
                        BookTable.ACTIVE},
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

                Cursor chapterCursor = db.query(ChapterTable.TABLE_NAME,
                        new String[]{ChapterTable.DURATION, ChapterTable.NAME, ChapterTable.PATH},
                        BookTable.ID + "=?",
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

                Cursor bookmarkCursor = db.query(BookmarkTable.TABLE_NAME,
                        new String[]{BookmarkTable.PATH, BookmarkTable.TIME, BookmarkTable.TITLE},
                        BookTable.ID + "=?", new String[]{String.valueOf(bookId)}
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
            ContentValues bookCv = BookTable.getContentValues(mutableBook);

            long bookId = db.insert(BookTable.TABLE_NAME, null, bookCv);
            mutableBook.setId(bookId);

            for (Chapter c : mutableBook.getChapters()) {
                ContentValues chapterCv = ChapterTable.getContentValues(c, mutableBook.getId());
                db.insert(ChapterTable.TABLE_NAME, null, chapterCv);
            }

            for (Bookmark b : mutableBook.getBookmarks()) {
                ContentValues bookmarkCv = BookmarkTable.getContentValues(b, mutableBook.getId());
                db.insert(BookmarkTable.TABLE_NAME, null, bookmarkCv);
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
                    ContentValues bookCv = BookTable.getContentValues(mutableBook);
                    db.update(BookTable.TABLE_NAME, bookCv, BookTable.ID + "=?", new String[]{String.valueOf(mutableBook.getId())});

                    // delete old chapters and replace them with new ones
                    db.delete(ChapterTable.TABLE_NAME, BookTable.ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                    for (Chapter c : mutableBook.getChapters()) {
                        ContentValues chapterCv = ChapterTable.getContentValues(c, mutableBook.getId());
                        db.insert(ChapterTable.TABLE_NAME, null, chapterCv);
                    }

                    // replace old bookmarks and replace them with new ones
                    db.delete(BookmarkTable.TABLE_NAME, BookTable.ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                    for (Bookmark b : mutableBook.getBookmarks()) {
                        ContentValues bookmarkCV = BookmarkTable.getContentValues(b, mutableBook.getId());
                        db.insert(BookmarkTable.TABLE_NAME, null, bookmarkCV);
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
                cv.put(BookTable.ACTIVE, 0);
                getWritableDatabase().update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
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
                cv.put(BookTable.ACTIVE, 1);
                getWritableDatabase().update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(mutableBook.getId())});
                break;
            }
        }
        activeBooks.add(new Book(mutableBook));
        COMMUNICATION.bookSetChanged(activeBooks);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        BookTable.onCreate(db);
        ChapterTable.onCreate(db);
        BookmarkTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            DataBaseUpgradeHelper upgradeHelper = new DataBaseUpgradeHelper(db, c);
            upgradeHelper.upgrade(oldVersion);
        } catch (InvalidPropertiesFormatException e) {
            L.e(TAG, "Error at upgrade", e);
            BookTable.dropTableIfExists(db);
            ChapterTable.dropTableIfExists(db);
            BookmarkTable.dropTableIfExists(db);
            onCreate(db);
        }
    }
}
