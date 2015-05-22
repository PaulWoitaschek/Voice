package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;

import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Validate;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 27;
    private static final String DATABASE_NAME = "autoBookDB";

    // tables
    private static final String TABLE_BOOK = "TABLE_BOOK";

    // book keys
    private static final String BOOK_ID = "BOOK_ID";
    private static final String BOOK_JSON = "BOOK_JSON";
    private static final String BOOK_ACTIVE = "BOOK_ACTIVE";
    private static final String LAST_TIME_BOOK_WAS_ACTIVE = "LAST_TIME_BOOK_WAS_ACTIVE";

    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_JSON + " TEXT NOT NULL, " +
            LAST_TIME_BOOK_WAS_ACTIVE + " INTEGER NOT NULL, " +
            BOOK_ACTIVE + " INTEGER NOT NULL)";
    private static final String TAG = DataBaseHelper.class.getSimpleName();
    private static DataBaseHelper instance;
    private final Context c;
    private final LocalBroadcastManager bcm;
    private final ArrayList<Book> activeBooks = new ArrayList<>();
    private final ArrayList<Book> orphanedBooks = new ArrayList<>();

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
        this.bcm = LocalBroadcastManager.getInstance(c);

        Cursor cursor = getReadableDatabase().query(TABLE_BOOK,
                new String[]{BOOK_ID, BOOK_JSON, BOOK_ACTIVE},
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                Book book = new Gson().fromJson(cursor.getString(1), Book.class);
                book.setId(cursor.getLong(0));
                if (cursor.getInt(2) == 1) {
                    activeBooks.add(book);
                } else {
                    orphanedBooks.add(book);
                }
            }
        } finally {
            cursor.close();
        }

        cleanOrphans();
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DataBaseHelper(c.getApplicationContext());
        }
        return instance;
    }

    /**
     * Deletes orphaned books if there are more than 20. Begin with the oldest one.
     */
    private void cleanOrphans() {
        final int MAX_ORPHANS = 20;
        if (orphanedBooks.size() + activeBooks.size() > 40 && orphanedBooks.size() > MAX_ORPHANS) {
            ArrayList<Book> orphansToRemove = new ArrayList<>();
            int amountToRemove = orphanedBooks.size() - MAX_ORPHANS;
            Cursor orphans = getWritableDatabase().query(
                    TABLE_BOOK,
                    new String[]{BOOK_ID}, //columns
                    BOOK_ACTIVE + "=?", //selection
                    new String[]{String.valueOf(0)}, // args
                    null, null,
                    LAST_TIME_BOOK_WAS_ACTIVE); // order by
            try {
                while (orphans.moveToNext()) {
                    for (int i = 0; i < amountToRemove; i++) {
                        long idToRemove = orphans.getLong(0);
                        for (Book b : orphanedBooks) {
                            if (b.getId() == idToRemove) {
                                orphansToRemove.add(b);
                            }
                        }
                    }
                }
            } finally {
                orphans.close();
            }
            for (Book bookToRemove : orphansToRemove) {
                getWritableDatabase().delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(bookToRemove.getId())});
                orphanedBooks.remove(bookToRemove);

                File coverFile = bookToRemove.getCoverFile();
                if (coverFile.exists() && coverFile.canWrite()) {
                    //noinspection ResultOfMethodCallIgnored
                    coverFile.delete();
                }
            }
        }
    }

    public synchronized void addBook(@NonNull Book book) {
        L.v(TAG, "addBook=" + book.getName());
        ContentValues cv = new ContentValues();
        cv.put(BOOK_JSON, new Gson().toJson(book));
        cv.put(BOOK_ACTIVE, 1);
        cv.put(LAST_TIME_BOOK_WAS_ACTIVE, System.currentTimeMillis());
        long bookId = getWritableDatabase().insert(TABLE_BOOK, null, cv);
        book.setId(bookId);

        activeBooks.add(book);

        sendBookSetChanged();
    }

    private void sendBookSetChanged() {
        bcm.sendBroadcast(new Intent(Communication.BOOK_SET_CHANGED));
    }

    @Nullable
    public synchronized Book getBook(long id) {
        for (Book b : activeBooks) {
            if (b.getId() == id)
                return new Book(b);
        }
        return null;
    }


    @NonNull
    public synchronized ArrayList<Book> getActiveBooks() {
        ArrayList<Book> copyBooks = new ArrayList<>();
        for (Book b : activeBooks) {
            copyBooks.add(new Book(b));
        }
        return copyBooks;
    }

    public synchronized ArrayList<Book> getOrphanedBooks() {
        ArrayList<Book> copyBooks = new ArrayList<>();
        for (Book b : orphanedBooks) {
            copyBooks.add(new Book(b));
        }
        return copyBooks;
    }

    public synchronized void updateBook(@NonNull Book bookToUpdate) {
        L.v(TAG, "updateBook=" + bookToUpdate.getName());
        new Validate().notEmpty(bookToUpdate.getChapters());

        int indexToUpdate = -1;
        for (int i = 0; i < activeBooks.size(); i++)
            if (activeBooks.get(i).getId() == bookToUpdate.getId())
                indexToUpdate = i;

        if (indexToUpdate != -1) {
            activeBooks.set(indexToUpdate, bookToUpdate);

            ContentValues cv = new ContentValues();
            cv.put(BOOK_JSON, new Gson().toJson(bookToUpdate));
            cv.put(LAST_TIME_BOOK_WAS_ACTIVE, System.currentTimeMillis());
            getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(bookToUpdate.getId())});

            sendBookSetChanged();
        } else {
            L.e(TAG, "Could not update book=" + bookToUpdate);
        }
    }

    public synchronized void hideBook(@NonNull Book book) {
        L.v(TAG, "hideBook=" + book.getName());

        int indexToHide = -1;
        for (int i = 0; i < activeBooks.size(); i++)
            if (activeBooks.get(i).getId() == book.getId())
                indexToHide = i;
        if (indexToHide == -1) {
            throw new AssertionError("This should not have happened. Tried to remove a not existing book");
        } else {
            activeBooks.remove(indexToHide);
            orphanedBooks.add(book);

            ContentValues cv = new ContentValues();
            cv.put(BOOK_JSON, new Gson().toJson(book));
            cv.put(BOOK_ACTIVE, 0);
            cv.put(LAST_TIME_BOOK_WAS_ACTIVE, System.currentTimeMillis());
            getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});

            sendBookSetChanged();
        }
    }

    public synchronized void reveilBook(@NonNull Book book) {
        Iterator<Book> orphanedBookIterator = orphanedBooks.iterator();
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().getId() == book.getId()) {
                orphanedBookIterator.remove();
                break;
            }
        }
        activeBooks.add(book);
        ContentValues cv = new ContentValues();
        cv.put(BOOK_JSON, new Gson().toJson(book));
        cv.put(BOOK_ACTIVE, 1);
        cv.put(LAST_TIME_BOOK_WAS_ACTIVE, System.currentTimeMillis());
        getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});

        sendBookSetChanged();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            DataBaseUpgradeHelper upgradeHelper = new DataBaseUpgradeHelper(db, c);
            while (oldVersion < newVersion) {
                switch (oldVersion) {
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
                        upgradeHelper.upgrade23();
                        break;
                    case 24:
                        upgradeHelper.upgrade24();
                        break;
                    case 25:
                        upgradeHelper.upgrade25();
                        break;
                    case 26:
                        upgradeHelper.upgrade26();
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
}
