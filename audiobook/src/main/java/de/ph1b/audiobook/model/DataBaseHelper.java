package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;

import de.ph1b.audiobook.utils.L;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 21;
    private static final String DATABASE_NAME = "autoBookDB";

    private static final String TABLE_BOOK = "TABLE_BOOK";
    private static final String BOOK_ID = "BOOK_ID";
    private static final String BOOK_SORT_ID = "BOOK_SORT_ID";
    private static final String BOOK_ROOT = "BOOK_ROOT";
    private static final String BOOK_NAME = "BOOK_NAME";
    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_SORT_ID + " INTEGER, " +
            BOOK_NAME + " TEXT NOT NULL, " +
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

    private static final String JSON_TIME = "time";
    private static final String JSON_BOOKMARK_TIME = "time";
    private static final String JSON_BOOKMARK_TITLE = "title";
    private static final String JSON_SPEED = "speed";
    private static final String JSON_EXTENSION = "-map.json";
    private final FileFilter jsonFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(JSON_EXTENSION);
        }
    };
    private static final String JSON_BOOKMARKS = "bookmarks";
    private static final String JSON_REL_PATH = "relPath";
    private static final String JSON_BOOKMARK_REL_PATH = "relPath";
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
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        long bookId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put(BOOK_ROOT, book.getRoot());
            cv.put(BOOK_NAME, book.getName());
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
        int time = getTimeFromBook(book.getRoot(), book.getName());
        String relPath = getRelPathFromBook(book.getRoot(), book.getName());

        float speed = getSpeed(book.getRoot(), book.getName());
        book.setPlaybackSpeed(speed);

        // if the current rel path is defect, use a new one
        boolean relPathExists = false;
        for (Chapter c : book.getChapters()) {
            if (c.getPath().equals(relPath)) {
                relPathExists = true;
            }
        }
        if (!relPathExists) {
            L.e(TAG, "rel path does not exist; use the first one!");
            relPath = book.getChapters().get(0).getPath();
            time = 0;
        }

        book.setPosition(time, relPath);
        ArrayList<Bookmark> bookmarks = getBookmarks(book.getRoot(), book.getName());
        Collections.sort(bookmarks, new BookmarkComparator(book.getChapters()));
        book.getBookmarks().addAll(bookmarks);
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

    @Nullable
    private Book getBook(long id, SQLiteDatabase db) {
        Cursor cursor = db.query(TABLE_BOOK,
                new String[]{BOOK_ROOT, BOOK_NAME, BOOK_SORT_ID},
                BOOK_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                String root = cursor.getString(0);
                String name = cursor.getString(1);
                long sortId = cursor.getLong(2);
                int currentTime = getTimeFromBook(root, name);
                ArrayList<Chapter> chapters = getChapters(id, db);
                ArrayList<Bookmark> unsafeBookmarks = getBookmarks(root, name);

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

                String relPath = getRelPathFromBook(root, name);
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

                float speed = getSpeed(root, name);

                return new Book(root, name, chapters, safeBookmarks, speed, id, sortId, currentTime, relPath);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void updateBook(Book newBook) {
        Book oldBookValues = getBook(newBook.getId(), getReadableDatabase());

        // if name has changed we must rename the file containing the track information
        if (oldBookValues != null && !oldBookValues.getName().equals(newBook.getName())) {
            File configFile = getConfigFile(oldBookValues.getRoot(), oldBookValues.getName());
            //noinspection ResultOfMethodCallIgnored
            configFile.renameTo(new File(configFile.getParent(), "." + newBook.getName() + JSON_EXTENSION));
        }

        ContentValues cv = new ContentValues();
        cv.put(BOOK_NAME, newBook.getName());
        cv.put(BOOK_SORT_ID, newBook.getSortId());

        JSONObject playingInformation = getPlayingInformation(newBook.getRoot(), newBook.getName());
        try {
            // updating time
            long time = newBook.getTime();
            String relPath = newBook.getCurrentChapter().getPath();
            playingInformation.put(JSON_TIME, time);
            playingInformation.put(JSON_REL_PATH, relPath);
            playingInformation.put(JSON_SPEED, String.valueOf(newBook.getPlaybackSpeed()));

            // updating bookmarks. Creating new array and overwriting old values
            playingInformation.put(JSON_BOOKMARKS, new JSONArray());
            JSONArray bookmarksJ = new JSONArray();
            for (Bookmark b : newBook.getBookmarks()) {
                JSONObject bookmarkJ = new JSONObject();
                bookmarkJ.put(JSON_BOOKMARK_TIME, b.getTime());
                bookmarkJ.put(JSON_BOOKMARK_TITLE, b.getTitle());
                bookmarkJ.put(JSON_REL_PATH, b.getPath());
                bookmarksJ.put(bookmarkJ);
            }
            playingInformation.put(JSON_BOOKMARKS, bookmarksJ);

            writeToFile(playingInformation, newBook);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(newBook.getId())});
    }

    private ArrayList<Chapter> getChapters(long bookId, SQLiteDatabase db) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        Cursor cursor = db.query(TABLE_CHAPTERS, new String[]{CHAPTER_PATH, CHAPTER_DURATION, CHAPTER_NAME},
                BOOK_ID + "=?",
                new String[]{String.valueOf(bookId)},
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

    public void updateBookmark(Bookmark newValues, Bookmark oldValues, Book book) {
        JSONObject rootJ = getPlayingInformation(book.getRoot(), book.getName());
        try {
            JSONArray bookmarksJ = rootJ.getJSONArray(JSON_BOOKMARKS);
            for (int i = 0; i < bookmarksJ.length(); i++) {
                JSONObject bookmark = bookmarksJ.getJSONObject(i);
                String title = bookmark.getString(JSON_BOOKMARK_TITLE);
                long time = bookmark.getLong(JSON_BOOKMARK_TIME);
                if (title.equals(oldValues.getTitle()) && time == oldValues.getTime()) {
                    bookmark.put(JSON_BOOKMARK_TITLE, newValues.getTitle());
                    bookmark.put(JSON_BOOKMARK_TIME, newValues.getTime());
                    writeToFile(rootJ, book);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private ArrayList<Bookmark> getBookmarks(String root, String bookName) {
        ArrayList<Bookmark> bookmarks = new ArrayList<>();
        try {
            JSONObject playingInformation = getPlayingInformation(root, bookName);
            JSONArray bookmarksJ = playingInformation.getJSONArray(JSON_BOOKMARKS);
            for (int i = 0; i < bookmarksJ.length(); i++) {
                JSONObject bookmarkJ = (JSONObject) bookmarksJ.get(i);
                int time = bookmarkJ.getInt(JSON_BOOKMARK_TIME);
                String title = bookmarkJ.getString(JSON_BOOKMARK_TITLE);
                String relPath = bookmarkJ.getString(JSON_BOOKMARK_REL_PATH);
                if (new File(root + "/" + relPath).exists()) {
                    bookmarks.add(new Bookmark(relPath, title, time));
                } else {
                    L.e(TAG, "Bookmark with filePath=" + root + "/" + relPath + " does not exist");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bookmarks;
    }

    private void writeToFile(JSONObject data, Book book) {
        File configFile = getConfigFile(book.getRoot(), book.getName());
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(configFile));
            outputStreamWriter.write(data.toString());
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getConfigFile(String rootPath, String bookName) {
        File configFile = new File(rootPath, "." + bookName + JSON_EXTENSION);
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configFile;
    }

    private String getRelPathFromBook(String root, String bookName) {
        JSONObject playingInformation = getPlayingInformation(root, bookName);
        try {
            return playingInformation.getString(JSON_REL_PATH);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getTimeFromBook(String root, String bookName) {
        JSONObject playingInformation = getPlayingInformation(root, bookName);
        try {
            return playingInformation.getInt(JSON_TIME);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }


    private float getSpeed(String root, String bookName) {
        JSONObject playingInformation = getPlayingInformation(root, bookName);
        try {
            return Float.valueOf(playingInformation.getString(JSON_SPEED));
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 1f;
    }

    private JSONObject getPlayingInformation(String root, String bookName) {
        JSONObject rootJ = new JSONObject();

        File configFile = getConfigFile(root, bookName);
        if (configFile.length() > 0) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
                StringBuilder stringBuilder = new StringBuilder();

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                bufferedReader.close();
                rootJ = new JSONObject(stringBuilder.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            // bookmarks key
            if (rootJ.has(JSON_BOOKMARKS)) {
                Object o = rootJ.get(JSON_BOOKMARKS);
                if (!(o instanceof JSONArray)) {
                    rootJ.put(JSON_BOOKMARKS, new JSONArray());
                }
            } else {
                rootJ.put(JSON_BOOKMARKS, new JSONArray());
            }

            // time key
            if (rootJ.has(JSON_TIME)) {
                Object o = rootJ.get(JSON_TIME);
                if (!(o instanceof Integer)) {
                    rootJ.put(JSON_TIME, 0);
                }
            } else {
                rootJ.put(JSON_TIME, 0);
            }

            // rel path
            if (rootJ.has(JSON_REL_PATH)) {
                Object o = rootJ.get(JSON_REL_PATH);
                if (!(o instanceof String)) {
                    rootJ.put(JSON_REL_PATH, "");
                }
            } else {
                rootJ.put(JSON_REL_PATH, "");
            }

            // speed
            if (rootJ.has(JSON_SPEED)) {
                Object o = rootJ.get(JSON_SPEED);
                if (!(o instanceof String)) {
                    rootJ.put(JSON_SPEED, "1.0");
                }
            } else {
                rootJ.put(JSON_SPEED, "1.0");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rootJ;
    }
}
