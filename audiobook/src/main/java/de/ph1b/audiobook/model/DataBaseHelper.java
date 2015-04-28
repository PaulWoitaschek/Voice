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

import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Validate;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 26;
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
    private final LocalBroadcastManager bcm;
    private ArrayList<Book> allBooks = null;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.c = c;
        this.bcm = LocalBroadcastManager.getInstance(c);
    }

    public static synchronized DataBaseHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DataBaseHelper(c.getApplicationContext());
        }
        return instance;
    }

    public synchronized void addBook(@NonNull Book book) {
        L.v(TAG, "addBook=" + book.getName());
        ContentValues cv = new ContentValues();
        cv.put(BOOK_JSON, new Gson().toJson(book));
        long bookId = getWritableDatabase().insert(TABLE_BOOK, null, cv);
        book.setId(bookId);

        getInternalBooks().add(book);

        sendBookSetChanged();
    }

    private void sendBookSetChanged() {
        bcm.sendBroadcast(new Intent(Communication.BOOK_SET_CHANGED));
    }

    @Nullable
    public synchronized Book getBook(long id) {
        for (Book b : getInternalBooks()) {
            if (b.getId() == id)
                return new Book(b);
        }
        return null;
    }

    private synchronized ArrayList<Book> getInternalBooks() {
        if (allBooks == null) {
            allBooks = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(TABLE_BOOK,
                    new String[]{BOOK_ID, BOOK_JSON},
                    null, null, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    Book book = new Gson().fromJson(cursor.getString(1), Book.class);
                    book.setId(cursor.getLong(0));
                    allBooks.add(book);
                }
            } finally {
                cursor.close();
            }
        }

        return allBooks;
    }

    @NonNull
    public synchronized ArrayList<Book> getAllBooks() {
        ArrayList<Book> internalBooks = getInternalBooks();
        ArrayList<Book> copyBooks = new ArrayList<>();
        for (Book b : internalBooks) {
            copyBooks.add(new Book(b));
        }

        return copyBooks;
    }

    public synchronized void updateBook(@NonNull Book bookToUpdate) {
        L.v(TAG, "updateBook=" + bookToUpdate.getName());
        new Validate().notEmpty(bookToUpdate.getChapters());

        int indexToUpdate = -1;
        ArrayList<Book> allBooks = getInternalBooks();
        for (int i = 0; i < allBooks.size(); i++)
            if (allBooks.get(i).getId() == bookToUpdate.getId())
                indexToUpdate = i;

        if (indexToUpdate != -1) {
            allBooks.set(indexToUpdate, bookToUpdate);

            ContentValues cv = new ContentValues();
            cv.put(BOOK_JSON, new Gson().toJson(bookToUpdate));
            getWritableDatabase().update(TABLE_BOOK, cv, BOOK_ID + "=?", new String[]{String.valueOf(bookToUpdate.getId())});

            sendBookSetChanged();
        } else {
            L.e(TAG, "Could not update book=" + bookToUpdate);
        }
    }

    public synchronized void deleteBook(@NonNull Book book) {
        L.v(TAG, "deleteBook=" + book.getName());

        int indexToDelete = -1;
        ArrayList<Book> allBooks = getInternalBooks();
        for (int i = 0; i < allBooks.size(); i++)
            if (allBooks.get(i).getId() == book.getId())
                indexToDelete = i;

        if (indexToDelete == -1) {
            L.e(TAG, "Could not delete book=" + book);
        } else {
            allBooks.remove(indexToDelete);

            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_BOOK, BOOK_ID + "=?", new String[]{String.valueOf(book.getId())});
            File coverFile = book.getCoverFile();
            if (coverFile.exists() && coverFile.canWrite()) {
                //noinspection ResultOfMethodCallIgnored
                coverFile.delete();
            }

            sendBookSetChanged();
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
                        break;
                    case 24:
                        DataBaseUpgradeHelper.upgrade24(db, c);
                        break;
                    case 25:
                        DataBaseUpgradeHelper.upgrade25(db);
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
