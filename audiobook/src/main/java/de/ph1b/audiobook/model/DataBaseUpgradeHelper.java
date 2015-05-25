package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;

import de.ph1b.audiobook.utils.L;


@SuppressWarnings("TryFinallyCanBeTryWithResources")
class DataBaseUpgradeHelper {

    private static final String TAG = DataBaseUpgradeHelper.class.getSimpleName();

    private final SQLiteDatabase db;
    private final Context c;

    public DataBaseUpgradeHelper(SQLiteDatabase db, Context c) {
        this.db = db;
        this.c = c;
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
    @SuppressWarnings("ConstantConditions")
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

                ArrayList<String> chapterNames = new ArrayList<>();
                ArrayList<Integer> chapterDurations = new ArrayList<>();
                ArrayList<String> chapterPaths = new ArrayList<>();

                Cursor mediaCursor = db.query(copyChapterTableName, new String[]{"CHAPTER_PATH", "CHAPTER_DURATION",
                                "CHAPTER_NAME"},
                        "BOOK_ID" + "=?", new String[]{String.valueOf(bookId)},
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

                    L.d(TAG, "upgrade24 restored book=" + book);
                    ContentValues cv = new ContentValues();
                    cv.put("BOOK_JSON", new Gson().toJson(book, Book.class));
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
                                    File.separator + "Android" + File.separator + "data" + File.separator + c.getPackageName(),
                                    newBookId + ".jpg");
                            if (!coverFile.getParentFile().exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                coverFile.getParentFile().mkdirs();
                            }
                            FileUtils.moveFile(coverFile, newCoverFile);
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
        ArrayList<JSONObject> allBooks = new ArrayList<>();
        Cursor cursor = db.query("TABLE_BOOK",
                new String[]{"BOOK_ID", "BOOK_JSON"},
                null, null, null, null, null);
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
            default:
                break;
        }
    }

}
