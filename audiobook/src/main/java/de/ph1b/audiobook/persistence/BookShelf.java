package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.utils.Communication;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * This is the helper for the apps database. All Database writing is done by an executor and the
 * helper holds a internal array of the books.
 *
 * @author Paul Woitaschek
 */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
@Singleton
public class BookShelf extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 32;
    private static final String DATABASE_NAME = "autoBookDB";
    private final Communication communication;
    private final Context c;
    private final List<Book> activeBooks;
    private final List<Book> orphanedBooks;

    @Inject
    public BookShelf(Context c, Communication communication) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
        this.communication = communication;

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
                        chapters.add(Chapter.of(chapterFile, chapterName, chapterDuration));
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
                        bookmarks.add(Bookmark.of(bookmarkFile, bookmarkTitle, bookmarkTime));
                    }
                } finally {
                    bookmarkCursor.close();
                }

                Book book = Book.builder(bookRoot, chapters, bookType, bookmarkCurrentMediaPath, bookName)
                        .time(bookTime)
                        .playbackSpeed(bookSpeed)
                        .author(bookAuthor)
                        .useCoverReplacement(bookUseCoverReplacement)
                        .bookmarks(ImmutableList.copyOf(bookmarks))
                        .id(bookId)
                        .build();

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

    public synchronized void addBook(@NonNull Book book) {
        Timber.v("addBook=%s", book.name());
        checkArgument(!book.chapters().isEmpty());

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues bookCv = BookTable.getContentValues(book);

            long bookId = db.insert(BookTable.TABLE_NAME, null, bookCv);
            book = Book.builder(book)
                    .id(bookId)
                    .build();

            for (Chapter c : book.chapters()) {
                ContentValues chapterCv = ChapterTable.getContentValues(c, book.id());
                db.insert(ChapterTable.TABLE_NAME, null, chapterCv);
            }

            for (Bookmark b : book.bookmarks()) {
                ContentValues bookmarkCv = BookmarkTable.getContentValues(b, book.id());
                db.insert(BookmarkTable.TABLE_NAME, null, bookmarkCv);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        activeBooks.add(book);
        communication.bookSetChanged(activeBooks);
    }


    @Nullable
    public synchronized Book getBook(long id) {
        for (Book b : activeBooks) {
            if (b.id() == id) {
                return b;
            }
        }
        return null;
    }


    @NonNull
    public synchronized List<Book> getActiveBooks() {
        return new ArrayList<>(activeBooks);
    }

    @NonNull
    public synchronized List<Book> getOrphanedBooks() {
        return new ArrayList<>(orphanedBooks);
    }

    public synchronized void updateBook(@NonNull Book book) {
        Timber.v("updateBook=%s", book.name());
        checkArgument(!book.chapters().isEmpty());

        ListIterator<Book> bookIterator = activeBooks.listIterator();
        while (bookIterator.hasNext()) {
            Book next = bookIterator.next();
            if (book.id() == next.id()) {
                bookIterator.set(book);

                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    // update book itself
                    ContentValues bookCv = BookTable.getContentValues(book);
                    db.update(BookTable.TABLE_NAME, bookCv, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});

                    // delete old chapters and replace them with new ones
                    db.delete(ChapterTable.TABLE_NAME, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                    for (Chapter c : book.chapters()) {
                        ContentValues chapterCv = ChapterTable.getContentValues(c, book.id());
                        db.insert(ChapterTable.TABLE_NAME, null, chapterCv);
                    }

                    // replace old bookmarks and replace them with new ones
                    db.delete(BookmarkTable.TABLE_NAME, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                    for (Bookmark b : book.bookmarks()) {
                        ContentValues bookmarkCV = BookmarkTable.getContentValues(b, book.id());
                        db.insert(BookmarkTable.TABLE_NAME, null, bookmarkCV);
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                break;
            }
        }

        communication.sendBookContentChanged(book);
    }

    public synchronized void hideBook(@NonNull Book book) {
        Timber.v("hideBook=%s", book.name());
        checkArgument(!book.chapters().isEmpty());

        ListIterator<Book> iterator = activeBooks.listIterator();
        while (iterator.hasNext()) {
            Book next = iterator.next();
            if (next.id() == book.id()) {
                iterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BookTable.ACTIVE, 0);
                getWritableDatabase().update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                break;
            }
        }
        orphanedBooks.add(book);
        communication.bookSetChanged(activeBooks);
    }

    public synchronized void revealBook(@NonNull Book book) {
        checkArgument(!book.chapters().isEmpty());

        Iterator<Book> orphanedBookIterator = orphanedBooks.iterator();
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().id() == book.id()) {
                orphanedBookIterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BookTable.ACTIVE, 1);
                getWritableDatabase().update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                break;
            }
        }
        activeBooks.add(book);
        communication.bookSetChanged(activeBooks);
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
            Timber.e(e, "Error at upgrade");
            BookTable.dropTableIfExists(db);
            ChapterTable.dropTableIfExists(db);
            BookmarkTable.dropTableIfExists(db);
            onCreate(db);
        }
    }
}
