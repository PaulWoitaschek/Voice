package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.ph1b.audiobook.utils.L;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 22;
    private static final String DATABASE_NAME = "autoBookDB";

    private static final String TABLE_BOOK = "TABLE_BOOK";
    private static final String BOOK_ID = "BOOK_ID";
    private static final String BOOK_SORT_ID = "BOOK_SORT_ID";
    private static final String BOOK_ROOT = "BOOK_ROOT";
    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_SORT_ID + " INTEGER, " +
            BOOK_ROOT + " TEXT NOT NULL)";

    private static final String TABLE_CHAPTERS = "TABLE_CHAPTERS";
    private static final String CHAPTER_ID = "CHAPTER_ID";
    private static final String CHAPTER_PATH = "CHAPTER_PATH";
    private static final String CHAPTER_DURATION = "CHAPTER_DURATION";
    private static final String CHAPTER_NAME = "CHAPTER_NAME";
    private static final String CREATE_TABLE_CHAPTERS = "CREATE TABLE " + TABLE_CHAPTERS + " ( " +
            CHAPTER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            CHAPTER_PATH + " TEXT NOT NULL, " +
            CHAPTER_DURATION + " INTEGER NOT NULL, " +
            CHAPTER_NAME + " TEXT NOT NULL, " +
            BOOK_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY(" + BOOK_ID + ") REFERENCES " + TABLE_BOOK + "(" + BOOK_ID + "))";


    private static final String TAG = DataBaseHelper.class.getSimpleName();
    private static DataBaseHelper instance;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DataBaseHelper(c);
        }
        return instance;
    }

    public void addBook(Book book) {
        // saving new values
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long bookId = Book.ID_UNKNOWN;
        try {
            ContentValues cv = new ContentValues();
            cv.put(BOOK_ROOT, book.getRoot());
            bookId = db.insert(TABLE_BOOK, null, cv);
            cv.put(BOOK_SORT_ID, bookId);
            db.update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(bookId)});

            for (Chapter c : book.getChapters()) {
                cv = new ContentValues();
                cv.put(CHAPTER_PATH, c.getPath());
                cv.put(BOOK_ID, bookId);
                cv.put(CHAPTER_DURATION, c.getDuration());
                cv.put(CHAPTER_NAME, c.getName());
                db.insert(TABLE_CHAPTERS, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        book.setId(bookId);
        book.setSortId(bookId);

        // retrieving existing values
        JSONHelper helper = new JSONHelper(getConfigFile(book.getRoot(), book.getChapters()));

        // name
        String name = helper.getName();
        if (!name.equals("")) {
            book.setName(name);
        } else {
            helper.setName(book.getName());
        }


        // relPath
        String relPath = helper.getRelPath();
        boolean relPathExists = false;
        for (Chapter c : book.getChapters()) {
            if (c.getPath().equals(relPath)) {
                relPathExists = true;
            }
        }

        // time
        int time = helper.getTime();
        if (!relPathExists) {
            time = 0;
            relPath = book.getChapters().get(0).getPath();
        }

        helper.setTime(time);
        helper.setRelPath(relPath);
        book.setPosition(time, relPath);

        // speed
        float speed = helper.getSpeed();
        book.setPlaybackSpeed(speed);

        // bookmarks
        ArrayList<Bookmark> unsafeBookmarks = helper.getBookmarks();
        ArrayList<Bookmark> safeBookmarks = new ArrayList<>();
        for (Bookmark b : unsafeBookmarks) {
            for (Chapter c : book.getChapters()) {
                if (b.getPath().equals(c.getPath())) {
                    safeBookmarks.add(b);
                    break;
                }
            }
        }
        Collections.sort(safeBookmarks, new BookmarkComparator(book.getChapters()));
        helper.setBookmarks(safeBookmarks);
        book.getBookmarks().addAll(safeBookmarks);

        helper.writeJSON();

        L.d(TAG, "added book to db:" + book);
    }

    public ArrayList<Book> getAllBooks() {
        ArrayList<Book> allBooks = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        Cursor cursor = db.query(TABLE_BOOK, new String[]{BOOK_ID}, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                allBooks.add(getBook(id, db));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            cursor.close();
        }

        Collections.sort(allBooks);

        return allBooks;
    }

    private File getConfigFile(String root, ArrayList<Chapter> chapters) {
        if (chapters.size() == 1) {
            String fileName = "." + chapters.get(0).getName() + JSONHelper.JSON_EXTENSION;
            return new File(root, fileName);
        } else {
            String fileName = "." + (new File(root).getName()) + JSONHelper.JSON_EXTENSION;
            return new File(root, fileName);
        }
    }

    @Nullable
    private Book getBook(long id, SQLiteDatabase db) {
        Cursor cursor = db.query(TABLE_BOOK,
                new String[]{BOOK_ROOT, BOOK_SORT_ID},
                BOOK_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                String root = cursor.getString(0);
                long sortId = cursor.getLong(1);
                ArrayList<Chapter> chapters = getChapters(id, db);

                JSONHelper helper = new JSONHelper(getConfigFile(root, chapters));

                int currentTime = helper.getTime();

                ArrayList<Bookmark> unsafeBookmarks = helper.getBookmarks();
                ArrayList<Bookmark> safeBookmarks = new ArrayList<>();
                for (Bookmark b : unsafeBookmarks) {
                    boolean bookmarkExists = false;
                    for (Chapter c : chapters) {
                        if (c.getPath().equals(b.getPath())) {
                            bookmarkExists = true;
                            break;
                        }
                    }
                    if (bookmarkExists) {
                        safeBookmarks.add(b);
                    } else {
                        L.e(TAG, "deleted bookmark=" + b + " because it was not in chapters=" + chapters);
                    }
                }

                String relPath = helper.getRelPath();
                boolean relPathExists = false;
                for (Chapter c : chapters) {
                    if (c.getPath().equals(relPath)) {
                        relPathExists = true;
                    }
                }
                if (!relPathExists) {
                    relPath = chapters.get(0).getPath();
                    currentTime = 0;
                }

                float speed = helper.getSpeed();
                String name = helper.getName();
                if (name.equals("")) {
                    if (chapters.size() == 1) {
                        String chapterPath = chapters.get(0).getPath();
                        name = chapterPath.substring(0, chapterPath.lastIndexOf("."));
                    } else {
                        name = new File(root).getName();
                    }
                }

                return new Book(root, name, chapters, safeBookmarks, speed, id, sortId, currentTime, relPath);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void updateBook(Book book) {
        JSONHelper helper = new JSONHelper(getConfigFile(book.getRoot(), book.getChapters()));

        helper.setTime(book.getTime());
        helper.setSpeed(book.getPlaybackSpeed());
        helper.setRelPath(book.getCurrentChapter().getPath());
        helper.setBookmarks(book.getBookmarks());
        helper.setName(book.getName());
        helper.writeJSON();

        ContentValues cv = new ContentValues();
        cv.put(BOOK_SORT_ID, book.getSortId());
        getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
    }

    private ArrayList<Chapter> getChapters(long bookId, SQLiteDatabase db) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        Cursor cursor = db.query(TABLE_CHAPTERS, new String[]{CHAPTER_PATH, CHAPTER_DURATION, CHAPTER_NAME},
                BOOK_ID + "=?", new String[]{String.valueOf(bookId)},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                int duration = cursor.getInt(1);
                String name = cursor.getString(2);
                chapters.add(new Chapter(path, name, duration));
            }
        } finally {
            cursor.close();
        }
        return chapters;
    }

    public void deleteBook(Book book) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOK);
        db.execSQL(CREATE_TABLE_CHAPTERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
        onCreate(db);
    }
}
