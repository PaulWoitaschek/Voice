package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
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

import de.ph1b.audiobook.interfaces.ForApplication;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.utils.Communication;
import timber.log.Timber;


/**
 * This is the helper for the apps database.
 *
 * @author Paul Woitaschek
 */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
@Singleton
public class BookShelf {

    private static final int BOOLEAN_TRUE = 1;
    private static final int BOOLEAN_FALSE = 0;
    private static final String KEY_CHAPTER_DURATIONS = "chapterDurations";
    private static final String KEY_CHAPTER_NAMES = "chapterNames";
    private static final String KEY_CHAPTER_PATHS = "chapterPaths";
    private static final String KEY_BOOKMARK_POSITIONS = "keyBookmarkPosition";
    private static final String KEY_BOOKMARK_TITLES = "keyBookmarkTitle";
    private static final String KEY_BOOKMARK_PATHS = "keyBookmarkPath";
    private static final String stringSeparator = "-~_";
    private static final String FULL_PROJECTION = "SELECT" +
            " bt." + BookTable.ID +
            ", bt." + BookTable.NAME +
            ", bt." + BookTable.AUTHOR +
            ", bt." + BookTable.CURRENT_MEDIA_PATH +
            ", bt." + BookTable.PLAYBACK_SPEED +
            ", bt." + BookTable.ROOT +
            ", bt." + BookTable.TIME +
            ", bt." + BookTable.TYPE +
            ", bt." + BookTable.USE_COVER_REPLACEMENT +
            ", bt." + BookTable.ACTIVE +
            ", ct." + KEY_CHAPTER_PATHS +
            ", ct." + KEY_CHAPTER_NAMES +
            ", ct." + KEY_CHAPTER_DURATIONS +
            ", bmt." + KEY_BOOKMARK_TITLES +
            ", bmt." + KEY_BOOKMARK_PATHS +
            ", bmt." + KEY_BOOKMARK_POSITIONS +
            " FROM " +
            BookTable.TABLE_NAME + " AS bt " +
            " left join" +
            "   (select " + ChapterTable.BOOK_ID + "," +
            "           group_concat(" + ChapterTable.PATH + ", '" + stringSeparator + "') as " + KEY_CHAPTER_PATHS + "," +
            "           group_concat(" + ChapterTable.DURATION + ") as " + KEY_CHAPTER_DURATIONS + "," +
            "           group_concat(" + ChapterTable.NAME + ", '" + stringSeparator + "') as " + KEY_CHAPTER_NAMES +
            "    from " + ChapterTable.TABLE_NAME +
            "    group by " + ChapterTable.BOOK_ID + ") AS ct on ct." + ChapterTable.BOOK_ID + " = bt." + BookTable.ID +
            " left join" +
            "    (select " + BookmarkTable.BOOK_ID + "," + "" +
            "            group_concat(" + BookmarkTable.TITLE + ", '" + stringSeparator + "') as " + KEY_BOOKMARK_TITLES + "," +
            "            group_concat(" + BookmarkTable.PATH + ", '" + stringSeparator + "') as " + KEY_BOOKMARK_PATHS + "," +
            "            group_concat(" + BookmarkTable.TIME + ") as " + KEY_BOOKMARK_POSITIONS +
            "     FROM " + BookmarkTable.TABLE_NAME +
            "     group by " + BookmarkTable.BOOK_ID + ") AS bmt on bmt." + BookmarkTable.BOOK_ID + " = bt." + BookTable.ID;
    private final Communication communication;
    private final List<Book> activeBooks;
    private final List<Book> orphanedBooks;
    private final SQLiteDatabase db;

    @Inject
    public BookShelf(@NonNull @ForApplication Context c, @NonNull Communication communication) {
        this.communication = communication;
        this.db = new InternalDb(c).getWritableDatabase();

        Cursor cursor = db.rawQuery(FULL_PROJECTION, null);
        try {
            activeBooks = new ArrayList<>();
            orphanedBooks = new ArrayList<>();
            while (cursor.moveToNext()) {
                boolean bookActive = cursor.getInt(cursor.getColumnIndexOrThrow(BookTable.ACTIVE)) == BOOLEAN_TRUE;
                Book book = byProjection(cursor);

                if (bookActive) {
                    activeBooks.add(book);
                } else {
                    orphanedBooks.add(book);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private static List<Bookmark> generateBookmarks(int[] position, String[] path, String[] title) {
        Preconditions.checkArgument(position.length == path.length && path.length == title.length,
                "Positions, path and title must have the same length but they are %d %d and %d",
                position.length, path.length, title.length);
        int length = position.length;
        List<Bookmark> bookmarks = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            bookmarks.add(Bookmark.of(new File(path[i]), title[i], position[i]));
        }
        return bookmarks;
    }

    private static List<Chapter> generateChapters(int[] position, String[] path, String[] title) {
        Preconditions.checkArgument(position.length == path.length && path.length == title.length,
                "Positions, path and title must have the same length but they are %d %d and %d",
                position.length, path.length, title.length);
        int length = position.length;
        List<Chapter> bookmarks = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            bookmarks.add(Chapter.of(new File(path[i]), title[i], position[i]));
        }
        return bookmarks;
    }

    private static int[] convertToStringArray(String[] in) {
        int[] out = new int[in.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = Integer.valueOf(in[i]);
        }
        return out;
    }

    private static Book byProjection(Cursor cursor) {
        String rawDurations = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAPTER_DURATIONS));
        String rawChapterNames = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAPTER_NAMES));
        String rawChapterPaths = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAPTER_PATHS));

        int[] chapterDurations = convertToStringArray(rawDurations.split(","));
        String[] chapterNames = rawChapterNames.split(stringSeparator);
        String[] chapterPaths = rawChapterPaths.split(stringSeparator);

        List<Chapter> chapters = generateChapters(chapterDurations, chapterPaths, chapterNames);
        Collections.sort(chapters);

        String rawBookmarkPositions = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKMARK_POSITIONS));
        String rawBookmarkPaths = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKMARK_PATHS));
        String rawBookmarkTitles = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKMARK_TITLES));

        int[] bookmarkPositions = rawBookmarkPositions == null ? new int[0] : convertToStringArray(rawBookmarkPositions.split(","));
        String[] bookmarkPaths = rawBookmarkPaths == null ? new String[0] : rawBookmarkPaths.split(stringSeparator);
        String[] bookmarkTitles = rawBookmarkTitles == null ? new String[0] : rawBookmarkTitles.split(stringSeparator);

        List<Bookmark> bookmarks = generateBookmarks(bookmarkPositions, bookmarkPaths, bookmarkTitles);
        Collections.sort(bookmarks);

        long bookId = cursor.getLong(cursor.getColumnIndexOrThrow(BookTable.ID));
        String bookName = cursor.getString(cursor.getColumnIndexOrThrow(BookTable.NAME));
        String bookAuthor = cursor.getString(cursor.getColumnIndexOrThrow(BookTable.AUTHOR));
        File currentPath = new File(cursor.getString(cursor.getColumnIndexOrThrow(BookTable.CURRENT_MEDIA_PATH)));
        float bookSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(BookTable.PLAYBACK_SPEED));
        String bookRoot = cursor.getString(cursor.getColumnIndexOrThrow(BookTable.ROOT));
        int bookTime = cursor.getInt(cursor.getColumnIndexOrThrow(BookTable.TIME));
        Book.Type bookType = Book.Type.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(BookTable.TYPE)));
        boolean bookUseCoverReplacement = cursor.getInt(cursor.getColumnIndexOrThrow(BookTable.USE_COVER_REPLACEMENT)) == BOOLEAN_TRUE;

        return Book.builder(bookRoot, chapters, bookType, currentPath, bookName)
                .id(bookId)
                .author(bookAuthor)
                .playbackSpeed(bookSpeed)
                .time(bookTime)
                .useCoverReplacement(bookUseCoverReplacement)
                .bookmarks(ImmutableList.copyOf(bookmarks))
                .build();
    }

    public synchronized void addBook(@NonNull Book book) {
        Timber.v("addBook=%s", book.name());

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

        ListIterator<Book> bookIterator = activeBooks.listIterator();
        while (bookIterator.hasNext()) {
            Book next = bookIterator.next();
            if (book.id() == next.id()) {
                bookIterator.set(book);

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

        ListIterator<Book> iterator = activeBooks.listIterator();
        while (iterator.hasNext()) {
            Book next = iterator.next();
            if (next.id() == book.id()) {
                iterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BookTable.ACTIVE, BOOLEAN_FALSE);
                db.update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                break;
            }
        }
        orphanedBooks.add(book);
        communication.bookSetChanged(activeBooks);
    }

    public synchronized void revealBook(@NonNull Book book) {
        Iterator<Book> orphanedBookIterator = orphanedBooks.iterator();
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().id() == book.id()) {
                orphanedBookIterator.remove();
                ContentValues cv = new ContentValues();
                cv.put(BookTable.ACTIVE, BOOLEAN_TRUE);
                db.update(BookTable.TABLE_NAME, cv, BookTable.ID + "=?", new String[]{String.valueOf(book.id())});
                break;
            }
        }
        activeBooks.add(book);
        communication.bookSetChanged(activeBooks);
    }

    private static class InternalDb extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 32;
        private static final String DATABASE_NAME = "autoBookDB";

        public InternalDb(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                DataBaseUpgradeHelper upgradeHelper = new DataBaseUpgradeHelper(db);
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
}
