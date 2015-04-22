package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DataBaseHelper extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "autoBookDB2";

    // tables
    private static final String TABLE_BOOK = "TABLE_BOOK";

    // book keys
    private static final String BOOK_ID = "BOOK_ID";
    private static final String BOOK_JSON = "BOOK_JSON";

    private static final String CREATE_TABLE_BOOK = "CREATE TABLE " + TABLE_BOOK + " ( " +
            BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            BOOK_JSON + " TEXT NOT NULL)";

    private static DataBaseHelper instance;

    private DataBaseHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK);
        onCreate(db);
    }
}
