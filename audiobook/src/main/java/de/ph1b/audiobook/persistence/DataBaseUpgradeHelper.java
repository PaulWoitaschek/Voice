package de.ph1b.audiobook.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import de.ph1b.audiobook.utils.App;
import timber.log.Timber;


@SuppressWarnings("TryFinallyCanBeTryWithResources")
class DataBaseUpgradeHelper {

    private final SQLiteDatabase db;

    public DataBaseUpgradeHelper(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Drops all tables and creates new ones.
     */
    private void upgrade23() {
        db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK");
        db.execSQL("DROP TABLE IF EXISTS TABLE_CHAPTERS");

        db.execSQL("CREATE TABLE TABLE_BOOK ( " +
                "BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BOOK_TYPE TEXT NOT NULL, " +
                "BOOK_ROOT TEXT NOT NULL)");
        db.execSQL("CREATE TABLE " + "TABLE_CHAPTERS" + " ( " +
                "CHAPTER_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "CHAPTER_PATH" + " TEXT NOT NULL, " +
                "CHAPTER_DURATION" + " INTEGER NOT NULL, " +
                "CHAPTER_NAME" + " TEXT NOT NULL, " +
                "BOOK_ID" + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + "BOOK_ID" + ") REFERENCES TABLE_BOOK(BOOK_ID))");
    }

    /**
     * Migrate the database so they will be stored as json objects
     *
     * @throws InvalidPropertiesFormatException if there is an internal data mismatch
     */
    @SuppressWarnings({"ConstantConditions", "CollectionWithoutInitialCapacity"})
    private void upgrade24() throws InvalidPropertiesFormatException {
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
            while (bookCursor.moveToNext()) {
                long bookId = bookCursor.getLong(0);
                String root = bookCursor.getString(1);
                String type = bookCursor.getString(2);

                Cursor mediaCursor = db.query(copyChapterTableName, new String[]{"CHAPTER_PATH", "CHAPTER_DURATION",
                                "CHAPTER_NAME"},
                        "BOOK_ID" + "=?", new String[]{String.valueOf(bookId)},
                        null, null, null);
                List<String> chapterNames = new ArrayList<>(mediaCursor.getCount());
                List<Integer> chapterDurations = new ArrayList<>(mediaCursor.getCount());
                List<String> chapterPaths = new ArrayList<>(mediaCursor.getCount());
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
                        String retString = Files.toString(configFile, Charsets.UTF_8);
                        if (!retString.isEmpty()) {
                            playingInformation = new JSONObject(retString);
                        }
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (playingInformation == null && backupFileValid) {
                        String retString = Files.toString(backupFile, Charsets.UTF_8);
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

                List<String> bookmarkRelPathsUnsafe = new ArrayList<>();
                List<String> bookmarkTitlesUnsafe = new ArrayList<>();
                List<Integer> bookmarkTimesUnsafe = new ArrayList<>();
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

                List<String> bookmarkRelPathsSafe = new ArrayList<>();
                List<String> bookmarkTitlesSafe = new ArrayList<>();
                List<Integer> bookmarkTimesSafe = new ArrayList<>();

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
                if (name.isEmpty()) {
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

                try {
                    JSONArray chapters = new JSONArray();
                    for (int i = 0; i < chapterPaths.size(); i++) {
                        JSONObject chapter = new JSONObject();
                        chapter.put("path", root + File.separator + chapterPaths.get(i));
                        chapter.put("duration", chapterDurations.get(i));
                        chapters.put(chapter);
                    }

                    JSONArray bookmarks = new JSONArray();
                    for (int i = 0; i < bookmarkRelPathsSafe.size(); i++) {
                        JSONObject bookmark = new JSONObject();
                        bookmark.put("mediaPath", root + File.separator + bookmarkRelPathsSafe.get(i));
                        bookmark.put("title", bookmarkTitlesSafe.get(i));
                        bookmark.put("time", bookmarkTimesSafe.get(i));
                        bookmarks.put(bookmark);
                    }

                    JSONObject book = new JSONObject();
                    book.put("root", root);
                    book.put("name", name);
                    book.put("chapters", chapters);
                    book.put("currentMediaPath", root + File.separator + currentPath);
                    book.put("type", type);
                    book.put("bookmarks", bookmarks);
                    book.put("useCoverReplacement", useCoverReplacement);
                    book.put("time", currentTime);
                    book.put("playbackSpeed", speed);

                    Timber.d("upgrade24 restored book=%s", book);
                    ContentValues cv = new ContentValues();
                    cv.put("BOOK_JSON", book.toString());
                    long newBookId = db.insert(newBookTable, null, cv);
                    book.put("id", newBookId);


                    // move cover file if possible
                    File coverFile;
                    if (chapterPaths.size() == 1) {
                        String fileName = "." + chapterNames.get(0) + ".jpg";
                        coverFile = new File(root, fileName);
                    } else {
                        String fileName = "." + (new File(root).getName()) + ".jpg";
                        coverFile = new File(root, fileName);
                    }
                    if (coverFile.exists() && coverFile.canWrite()) {
                        try {
                            File newCoverFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    File.separator + "Android" + File.separator + "data" + File.separator + App.getComponent().getContext().getPackageName(),
                                    newBookId + ".jpg");
                            if (!coverFile.getParentFile().exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                coverFile.getParentFile().mkdirs();
                            }
                            Files.move(coverFile, newCoverFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    throw new InvalidPropertiesFormatException(e);
                }
            }
        } finally {
            bookCursor.close();
        }
    }


    /**
     * A previous version caused empty books to be added. So we delete them now.
     */
    private void upgrade25() throws InvalidPropertiesFormatException {

        // get all books
        Cursor cursor = db.query("TABLE_BOOK",
                new String[]{"BOOK_ID", "BOOK_JSON"},
                null, null, null, null, null);
        List<JSONObject> allBooks = new ArrayList<>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                String content = cursor.getString(1);
                JSONObject book = new JSONObject(content);
                book.put("id", cursor.getLong(0));
                allBooks.add(book);
            }
        } catch (JSONException e) {
            throw new InvalidPropertiesFormatException(e);
        } finally {
            cursor.close();
        }

        // delete empty books
        try {
            for (JSONObject b : allBooks) {
                JSONArray chapters = b.getJSONArray("chapters");
                if (chapters.length() == 0) {
                    db.delete("TABLE_BOOK", "BOOK_ID" + "=?", new String[]{String.valueOf(b.get("id"))});
                }
            }
        } catch (JSONException e) {
            throw new InvalidPropertiesFormatException(e);
        }
    }


    /**
     * Adds a new column indicating if the book should be actively shown or hidden.
     */
    private void upgrade26() {
        String copyBookTableName = "TABLE_BOOK_COPY";
        db.execSQL("DROP TABLE IF EXISTS " + copyBookTableName);
        db.execSQL("ALTER TABLE TABLE_BOOK RENAME TO " + copyBookTableName);
        db.execSQL("CREATE TABLE " + "TABLE_BOOK" + " ( " +
                "BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BOOK_JSON" + " TEXT NOT NULL, " +
                "LAST_TIME_BOOK_WAS_ACTIVE" + " INTEGER NOT NULL, " +
                "BOOK_ACTIVE" + " INTEGER NOT NULL)");

        Cursor cursor = db.query(copyBookTableName, new String[]{"BOOK_JSON"}, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                ContentValues cv = new ContentValues();
                cv.put("BOOK_JSON", cursor.getString(0));
                cv.put("BOOK_ACTIVE", 1);
                cv.put("LAST_TIME_BOOK_WAS_ACTIVE", System.currentTimeMillis());
                db.insert("TABLE_BOOK", null, cv);
            }
        } finally {
            cursor.close();
        }
    }


    /**
     * Deletes the table if that failed previously due to a bug in {@link #upgrade26()}
     */
    private void upgrade27() {
        db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK_COPY");
    }


    /**
     * Adds
     *
     * @throws InvalidPropertiesFormatException
     */
    private void upgrade28() throws InvalidPropertiesFormatException {
        Timber.d("upgrade28");
        Cursor cursor = db.query("TABLE_BOOK", new String[]{"BOOK_JSON", "BOOK_ID"}, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                JSONObject book = new JSONObject(cursor.getString(0));
                JSONArray chapters = book.getJSONArray("chapters");
                for (int i = 0; i < chapters.length(); i++) {
                    JSONObject chapter = chapters.getJSONObject(i);
                    String fileName = new File(chapter.getString("path")).getName();
                    int dotIndex = fileName.lastIndexOf(".");
                    String chapterName;
                    if (dotIndex > 0) {
                        chapterName = fileName.substring(0, dotIndex);
                    } else {
                        chapterName = fileName;
                    }
                    chapter.put("name", chapterName);
                }
                ContentValues cv = new ContentValues();
                Timber.d("so saving book=%s", book.toString());
                cv.put("BOOK_JSON", book.toString());
                db.update("TABLE_BOOK", cv, "BOOK_ID" + "=?", new String[]{String.valueOf(cursor.getLong(1))});
            }
        } catch (JSONException e) {
            throw new InvalidPropertiesFormatException(e);
        } finally {
            cursor.close();
        }
    }

    private void upgrade29() {
        Timber.d("upgrade29");

        // fetching old contents
        Cursor cursor = db.query("TABLE_BOOK", new String[]{"BOOK_JSON", "BOOK_ACTIVE"},
                null, null, null, null, null);
        List<String> bookContents = new ArrayList<>(cursor.getCount());
        List<Boolean> activeMapping = new ArrayList<>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                bookContents.add(cursor.getString(0));
                activeMapping.add(cursor.getInt(1) == 1);
            }
        } finally {
            cursor.close();
        }
        db.execSQL("DROP TABLE TABLE_BOOK");

        // tables
        final String TABLE_BOOK = "tableBooks";
        final String TABLE_CHAPTERS = "tableChapters";
        final String TABLE_BOOKMARKS = "tableBookmarks";

        // book keys
        final String BOOK_ID = "bookId";
        final String BOOK_NAME = "bookName";
        final String BOOK_AUTHOR = "bookAuthor";
        final String BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath";
        final String BOOK_PLAYBACK_SPEED = "bookSpeed";
        final String BOOK_ROOT = "bookRoot";
        final String BOOK_TIME = "bookTime";
        final String BOOK_TYPE = "bookType";
        final String BOOK_USE_COVER_REPLACEMENT = "bookUseCoverReplacement";
        final String BOOK_ACTIVE = "BOOK_ACTIVE";

        // chapter keys
        final String CHAPTER_DURATION = "chapterDuration";
        final String CHAPTER_NAME = "chapterName";
        final String CHAPTER_PATH = "chapterPath";

        // bookmark keys
        final String BOOKMARK_TIME = "bookmarkTime";
        final String BOOKMARK_PATH = "bookmarkPath";
        final String BOOKMARK_TITLE = "bookmarkTitle";

        // create strings
        final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
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

        final String CREATE_TABLE_CHAPTERS = "CREATE TABLE " + TABLE_CHAPTERS + " ( " +
                CHAPTER_DURATION + " INTEGER NOT NULL, " +
                CHAPTER_NAME + " TEXT NOT NULL, " +
                CHAPTER_PATH + " TEXT NOT NULL, " +
                BOOK_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + TABLE_BOOK + "(" + BOOK_ID + "))";

        final String CREATE_TABLE_BOOKMARKS = "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
                BOOKMARK_PATH + " TEXT NOT NULL, " +
                BOOKMARK_TITLE + " TEXT NOT NULL, " +
                BOOKMARK_TIME + " INTEGER NOT NULL, " +
                BOOK_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " + TABLE_BOOK + "(" + BOOK_ID + "))";

        // drop tables in case they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);

        // create new tables
        db.execSQL(CREATE_TABLE_BOOK);
        db.execSQL(CREATE_TABLE_CHAPTERS);
        db.execSQL(CREATE_TABLE_BOOKMARKS);

        for (int i = 0; i < bookContents.size(); i++) {
            String bookJson = bookContents.get(i);
            boolean bookActive = activeMapping.get(i);

            try {

                JSONObject bookObj = new JSONObject(bookJson);
                JSONArray bookmarks = bookObj.getJSONArray("bookmarks");
                JSONArray chapters = bookObj.getJSONArray("chapters");
                String currentMediaPath = bookObj.getString("currentMediaPath");
                String bookName = bookObj.getString("name");
                float speed = (float) bookObj.getDouble("playbackSpeed");
                String root = bookObj.getString("root");
                int time = bookObj.getInt("time");
                String type = bookObj.getString("type");
                boolean useCoverReplacement = bookObj.getBoolean("useCoverReplacement");

                ContentValues bookCV = new ContentValues();
                bookCV.put(BOOK_CURRENT_MEDIA_PATH, currentMediaPath);
                bookCV.put(BOOK_NAME, bookName);
                bookCV.put(BOOK_PLAYBACK_SPEED, speed);
                bookCV.put(BOOK_ROOT, root);
                bookCV.put(BOOK_TIME, time);
                bookCV.put(BOOK_TYPE, type);
                bookCV.put(BOOK_USE_COVER_REPLACEMENT, useCoverReplacement ? 1 : 0);
                bookCV.put(BOOK_ACTIVE, bookActive ? 1 : 0);

                long bookId = db.insert(TABLE_BOOK, null, bookCV);


                for (int j = 0; j < chapters.length(); j++) {
                    JSONObject chapter = chapters.getJSONObject(j);
                    int chapterDuration = chapter.getInt("duration");
                    String chapterName = chapter.getString("name");
                    String chapterPath = chapter.getString("path");

                    ContentValues chapterCV = new ContentValues();
                    chapterCV.put(CHAPTER_DURATION, chapterDuration);
                    chapterCV.put(CHAPTER_NAME, chapterName);
                    chapterCV.put(CHAPTER_PATH, chapterPath);
                    chapterCV.put(BOOK_ID, bookId);

                    db.insert(TABLE_CHAPTERS, null, chapterCV);
                }

                for (int j = 0; j < bookmarks.length(); j++) {
                    JSONObject bookmark = bookmarks.getJSONObject(j);
                    int bookmarkTime = bookmark.getInt("time");
                    String bookmarkPath = bookmark.getString("mediaPath");
                    String bookmarkTitle = bookmark.getString("title");

                    ContentValues bookmarkCV = new ContentValues();
                    bookmarkCV.put(BOOKMARK_PATH, bookmarkPath);
                    bookmarkCV.put(BOOKMARK_TITLE, bookmarkTitle);
                    bookmarkCV.put(BOOKMARK_TIME, bookmarkTime);
                    bookmarkCV.put(BOOK_ID, bookId);

                    db.insert(TABLE_BOOKMARKS, null, bookmarkCV);
                }
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    /**
     * Queries through all books and removes the ones that were added empty by a bug.
     */
    private void upgrade30() {
        // book keys
        final String BOOK_ID = "bookId";
        final String TABLE_BOOK = "tableBooks";
        final String TABLE_CHAPTERS = "tableChapters";

        Cursor bookCursor = db.query(TABLE_BOOK,
                new String[]{BOOK_ID},
                null, null, null, null, null);
        try {
            while (bookCursor.moveToNext()) {
                long bookId = bookCursor.getLong(0);

                int chapterCount = 0;
                Cursor chapterCursor = db.query(TABLE_CHAPTERS,
                        null,
                        BOOK_ID + "=?",
                        new String[]{String.valueOf(bookId)},
                        null, null, null);
                try {
                    while (chapterCursor.moveToNext()) {
                        chapterCount++;
                    }
                } finally {
                    chapterCursor.close();
                }
                if (chapterCount == 0) {
                    db.delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(bookId)});
                }
            }
        } finally {
            bookCursor.close();
        }
    }

    /**
     * Corrects media paths that have been falsely set.
     */
    private void upgrade31() {
        final String BOOK_ID = "bookId";
        final String TABLE_BOOK = "tableBooks";
        final String TABLE_CHAPTERS = "tableChapters";
        final String BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath";
        final String CHAPTER_PATH = "chapterPath";

        Cursor bookCursor = db.query(TABLE_BOOK,
                new String[]{BOOK_ID, BOOK_CURRENT_MEDIA_PATH},
                null, null, null, null, null);
        try {
            while (bookCursor.moveToNext()) {
                long bookId = bookCursor.getLong(0);
                String bookmarkCurrentMediaPath = bookCursor.getString(1);

                Cursor chapterCursor = db.query(TABLE_CHAPTERS,
                        new String[]{CHAPTER_PATH},
                        BOOK_ID + "=?",
                        new String[]{String.valueOf(bookId)},
                        null, null, null);
                List<String> chapterPaths = new ArrayList<>(chapterCursor.getCount());
                try {
                    while (chapterCursor.moveToNext()) {
                        String chapterPath = chapterCursor.getString(0);
                        chapterPaths.add(chapterPath);
                    }
                } finally {
                    chapterCursor.close();
                }

                if (chapterPaths.isEmpty()) {
                    db.delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(bookId)});
                } else {
                    boolean mediaPathValid = false;
                    for (String s : chapterPaths) {
                        if (s.equals(bookmarkCurrentMediaPath)) {
                            mediaPathValid = true;
                        }
                    }
                    if (!mediaPathValid) {
                        ContentValues cv = new ContentValues();
                        cv.put(BOOK_CURRENT_MEDIA_PATH, chapterPaths.get(0));
                        db.update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(bookId)});
                    }
                }
            }
        } finally {
            bookCursor.close();
        }
    }

    public void upgrade(int fromVersion) throws InvalidPropertiesFormatException {
        switch (fromVersion) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                upgrade23();
            case 24:
                upgrade24();
            case 25:
                upgrade25();
            case 26:
                upgrade26();
            case 27:
                upgrade27();
            case 28:
                upgrade28();
            case 29:
                upgrade29();
            case 30:
                upgrade30();
            case 31:
                upgrade31();
            default:
                break;
        }
    }

}
