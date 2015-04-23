package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;

import de.ph1b.audiobook.utils.L;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 25;
    private static final String DATABASE_NAME = "autoBookDB";

    // tables
    private static final String TABLE_BOOK = "TABLE_BOOK";

    // book keys
    private static final String BOOK_ID = "BOOK_ID";
    private static final String BOOK_JSON = "BOOK_JSON";

    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_JSON + " TEXT NOT NULL)";
    private static final String TAG = DataBaseHelper.class.getSimpleName();
    private static DataBaseHelper instance;
    private final Context c;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DataBaseHelper(c.getApplicationContext());
        }
        return instance;
    }

    public void addBook(@NonNull Book book) {
        ContentValues cv = new ContentValues();
        cv.put(BOOK_JSON, new Gson().toJson(book));
        long bookId = getWritableDatabase().insert(TABLE_BOOK, null, cv);
        book.setId(bookId);
    }

    @NonNull
    public ArrayList<Book> getAllBooks() {
        ArrayList<Book> allBooks = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        Cursor cursor = db.query(TABLE_BOOK, new String[]{BOOK_ID, BOOK_JSON}, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                Book book = new Gson().fromJson(cursor.getString(1), Book.class);
                book.setId(cursor.getLong(0));
                allBooks.add(book);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            cursor.close();
        }

        Collections.sort(allBooks);

        return allBooks;
    }

    public void updateBook(@NonNull Book book) {
        ContentValues cv = new ContentValues();
        cv.put(BOOK_JSON, new Gson().toJson(book));
        getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
    }

    public void deleteBook(@NonNull Book book) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
        File coverFile = book.getCoverFile();
        if (coverFile.exists() && coverFile.canWrite()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.delete();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            while (oldVersion < newVersion) {
                switch (oldVersion) {
                    case 24:
                        upgrade24(db);
                        break;
                    default:
                        break;
                }
                oldVersion++;
            }
        } catch (InvalidPropertiesFormatException e) {
            L.e(TAG, "Error at upgrade", e);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
            db.execSQL(CREATE_TABLE_BOOK);
        }
    }

    private void upgrade24(SQLiteDatabase db) throws InvalidPropertiesFormatException {
        String copyBookTableName = "TABLE_BOOK_COPY";
        String copyChapterTableName = "TABLE_CHAPTERS_COPY";

        db.execSQL("ALTER TABLE TABLE_BOOK RENAME TO " + copyBookTableName);
        db.execSQL("ALTER TABLE TABLE_CHAPTERS RENAME TO " + copyChapterTableName);

        String newBookTable = "TABLE_BOOK";
        final String CREATE_TABLE_BOOK = "CREATE TABLE " + newBookTable + " ( " +
                "BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BOOK_JSON" + " TEXT NOT NULL)";
        db.execSQL(CREATE_TABLE_BOOK);


        Cursor bookCursor = db.query(copyBookTableName,
                new String[]{"BOOK_ID", "BOOK_ROOT", "BOOK_TYPE"},
                null, null, null, null, null);
        try {
            while(bookCursor.moveToNext()) {
                long bookId = bookCursor.getLong(0);
                String root = bookCursor.getString(1);
                String type = bookCursor.getString(2);

                ArrayList<String> chapterNames = new ArrayList<>();
                ArrayList<Integer> chapterDurations = new ArrayList<>();
                ArrayList<String> chapterPaths = new ArrayList<>();

                Cursor mediaCursor = db.query(copyChapterTableName, new String[]{"CHAPTER_PATH", "CHAPTER_DURATION",
                                "CHAPTER_NAME"},
                        BOOK_ID + "=?", new String[]{String.valueOf(bookId)},
                        null, null, null);
                try {
                    while (mediaCursor.moveToNext()) {
                        chapterPaths.add(mediaCursor.getString(0));
                        chapterDurations.add(mediaCursor.getInt(1));
                        chapterNames.add(mediaCursor.getString(2));
                    }
                } finally {
                    mediaCursor.close();
                }

                File configFile;
                switch (type) {
                    case "COLLECTION_FILE":
                    case "SINGLE_FILE":
                        configFile = new File(root, "." + chapterNames.get(0) + "-map.json");
                        break;
                    case "COLLECTION_FOLDER":
                    case "SINGLE_FOLDER":
                        configFile = new File(root, "." + (new File(root).getName()) + "-map.json");
                        break;
                    default:
                        throw new InvalidPropertiesFormatException("Upgrade failed due to unknown type=" + type);
                }
                File backupFile = new File(configFile.getAbsolutePath() + ".backup");

                boolean configFileValid = configFile.exists() && configFile.canRead()
                        && configFile.length() > 0;
                boolean backupFileValid = backupFile.exists() && backupFile.canRead()
                        && backupFile.length() > 0;


                JSONObject playingInformation = null;
                try {
                    if (configFileValid) {
                        String retString = FileUtils.readFileToString(configFile);
                        if (retString.length() > 0)
                            playingInformation = new JSONObject(retString);
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (playingInformation == null && backupFileValid) {
                        String retString = FileUtils.readFileToString(backupFile);
                        playingInformation = new JSONObject(retString);
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                if (playingInformation == null) {
                    throw new InvalidPropertiesFormatException("Could not fetch information");
                }

                final String JSON_TIME = "time";
                final String JSON_BOOKMARK_TIME = "time";
                final String JSON_BOOKMARK_TITLE = "title";
                final String JSON_SPEED = "speed";
                final String JSON_NAME = "name";
                final String JSON_BOOKMARKS = "bookmarks";
                final String JSON_REL_PATH = "relPath";
                final String JSON_BOOKMARK_REL_PATH = "relPath";
                final String JSON_USE_COVER_REPLACEMENT = "useCoverReplacement";

                int currentTime = 0;
                try {
                    currentTime = playingInformation.getInt(JSON_TIME);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ArrayList<String> bookmarkRelPathsUnsafe = new ArrayList<>();
                ArrayList<String> bookmarkTitlesUnsafe = new ArrayList<>();
                ArrayList<Integer> bookmarkTimesUnsafe = new ArrayList<>();
                try {
                    JSONArray bookmarksJ = playingInformation.getJSONArray(JSON_BOOKMARKS);
                    for (int i = 0; i < bookmarksJ.length(); i++) {
                        JSONObject bookmarkJ = (JSONObject) bookmarksJ.get(i);
                        bookmarkTimesUnsafe.add(bookmarkJ.getInt(JSON_BOOKMARK_TIME));
                        bookmarkTitlesUnsafe.add(bookmarkJ.getString(JSON_BOOKMARK_TITLE));
                        bookmarkRelPathsUnsafe.add(bookmarkJ.getString(JSON_BOOKMARK_REL_PATH));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    bookmarkRelPathsUnsafe.clear();
                    bookmarkTitlesUnsafe.clear();
                    bookmarkTimesUnsafe.clear();
                }

                ArrayList<String> bookmarkRelPathsSafe = new ArrayList<>();
                ArrayList<String> bookmarkTitlesSafe = new ArrayList<>();
                ArrayList<Integer> bookmarkTimesSafe = new ArrayList<>();

                for (int i = 0; i < bookmarkRelPathsUnsafe.size(); i++) {
                    boolean bookmarkExists = false;
                    for (String chapterPath : chapterPaths) {
                        if (chapterPath.equals(bookmarkRelPathsUnsafe.get(i))) {
                            bookmarkExists = true;
                            break;
                        }
                    }
                    if (bookmarkExists) {
                        bookmarkRelPathsSafe.add(bookmarkRelPathsUnsafe.get(i));
                        bookmarkTitlesSafe.add(bookmarkTitlesUnsafe.get(i));
                        bookmarkTimesSafe.add(bookmarkTimesUnsafe.get(i));
                    }
                }

                String currentPath = "";
                try {
                    currentPath = playingInformation.getString(JSON_REL_PATH);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                boolean relPathExists = false;
                for (String chapterPath : chapterPaths) {
                    if (chapterPath.equals(currentPath)) {
                        relPathExists = true;
                    }
                }
                if (!relPathExists) {
                    currentPath = chapterPaths.get(0);
                    currentTime = 0;
                }

                float speed = 1.0f;
                try {
                    speed = Float.valueOf(playingInformation.getString(JSON_SPEED));
                } catch (JSONException | NumberFormatException e) {
                    e.printStackTrace();
                }

                String name = "";
                try {
                    name = playingInformation.getString(JSON_NAME);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (name.equals("")) {
                    if (chapterPaths.size() == 1) {
                        String chapterPath = chapterPaths.get(0);
                        name = chapterPath.substring(0, chapterPath.lastIndexOf("."));
                    } else {
                        name = new File(root).getName();
                    }
                }

                boolean useCoverReplacement = false;
                try {
                    useCoverReplacement = playingInformation.getBoolean(JSON_USE_COVER_REPLACEMENT);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ArrayList<Chapter> chapters = new ArrayList<>();
                for (int i = 0; i < chapterPaths.size(); i++) {
                    chapters.add(new Chapter(root + File.separator + chapterPaths.get(i),
                            chapterNames.get(i), chapterDurations.get(i)));
                }

                ArrayList<Bookmark> bookmarks = new ArrayList<>();
                for (int i = 0; i < bookmarkRelPathsSafe.size(); i++) {
                    bookmarks.add(new Bookmark(root + File.separator + bookmarkRelPathsSafe.get(i),
                            bookmarkTitlesSafe.get(i), bookmarkTimesSafe.get(i)));
                }

                Book book = new Book(root, name, chapters, root + File.separator + currentPath,
                        Book.Type.valueOf(type), bookmarks, c);
                book.setUseCoverReplacement(useCoverReplacement);
                book.setPosition(currentTime, root + File.separator + currentPath);
                book.setPlaybackSpeed(speed);
                L.d(TAG, "upgrade24 restored book=" + book);
                ContentValues cv = new ContentValues();
                cv.put(BOOK_JSON, new Gson().toJson(book, Book.class));
                db.insert(newBookTable, null, cv);
            }
        } finally {
            bookCursor.close();
        }
    }
}
