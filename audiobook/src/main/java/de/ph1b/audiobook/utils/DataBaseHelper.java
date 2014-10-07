package de.ph1b.audiobook.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "audioBookDB";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_MEDIA = "TABLE_MEDIA";
    private static final String TABLE_BOOKS = "TABLE_BOOKS";

    private static final String KEY_MEDIA_ID = "KEY_MEDIA_ID";
    private static final String KEY_MEDIA_PATH = "KEY_MEDIA_PATH";
    private static final String KEY_MEDIA_NAME = "KEY_MEDIA_NAME";
    private static final String KEY_MEDIA_POSITION = "KEY_MEDIA_POSITION";
    private static final String KEY_MEDIA_DURATION = "KEY_MEDIA_DURATION";
    private static final String KEY_MEDIA_BOOK_ID = "KEY_MEDIA_BOOK_ID";

    private static final String KEY_BOOK_ID = "KEY_BOOK_ID";
    private static final String KEY_BOOK_NAME = "KEY_BOOK_NAME";
    private static final String KEY_BOOK_COVER = "KEY_BOOK_COVER";
    private static final String KEY_BOOK_POSITION = "KEY_BOOK_POSITION";
    private static final String KEY_BOOK_THUMB = "KEY_BOOK_THUMB";

    private static DataBaseHelper instance;

    public static synchronized DataBaseHelper getInstance(Context context) {
        if (instance == null)
            instance = new DataBaseHelper(context);
        return instance;
    }

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + " ( " +
                    KEY_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_MEDIA_PATH + " TEXT, " +
                    KEY_MEDIA_NAME + " TEXT, " +
                    KEY_MEDIA_POSITION + " INTEGER, " +
                    KEY_MEDIA_DURATION + " INTEGER, " +
                    KEY_MEDIA_BOOK_ID + " INTEGER" +
                    ")";

            db.execSQL(CREATE_MEDIA_TABLE);

            String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " ( " +
                    KEY_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_BOOK_NAME + " TEXT, " +
                    KEY_BOOK_COVER + " TEXT, " +
                    KEY_BOOK_THUMB + " TEXT, " +
                    KEY_BOOK_POSITION + " INTEGER"
                    + ")";
            db.execSQL(CREATE_BOOK_TABLE);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (BuildConfig.DEBUG) Log.d("dbh", "onUpgrade called");
        if (oldVersion == 1 && newVersion == 2) {
            db.beginTransaction();
            try {
                //first rename old tables
                db.execSQL("ALTER TABLE mediaTable RENAME TO tempMediaTable");
                db.execSQL("ALTER TABLE bookTable RENAME TO tempBookTable");

                //now create new tables
                String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + " ( " +
                        KEY_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        KEY_MEDIA_PATH + " TEXT, " +
                        KEY_MEDIA_NAME + " TEXT, " +
                        KEY_MEDIA_POSITION + " INTEGER, " +
                        KEY_MEDIA_DURATION + " INTEGER, " +
                        KEY_MEDIA_BOOK_ID + " INTEGER" +
                        ")";
                db.execSQL(CREATE_MEDIA_TABLE);
                String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " ( " +
                        KEY_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        KEY_BOOK_NAME + " TEXT, " +
                        KEY_BOOK_COVER + " TEXT, " +
                        KEY_BOOK_THUMB + " TEXT, " +
                        KEY_BOOK_POSITION + " INTEGER"
                        + ")";
                db.execSQL(CREATE_BOOK_TABLE);

                //now getting book table
                Cursor bookTableCursor = db.query("tempBookTable",
                        new String[]{"bookName", "bookCover", "bookMediaContaining", "bookPosition", "bookThumb"},
                        null, null, null, null, null);
                while (bookTableCursor.moveToNext()) {
                    String bookName = bookTableCursor.getString(0);
                    String bookCover = bookTableCursor.getString(1);
                    String bookMediaContaining = bookTableCursor.getString(2);
                    int bookPosition = bookTableCursor.getInt(3);
                    String bookThumb = bookTableCursor.getString(4);

                    //adding book in new table
                    ContentValues bookValues = new ContentValues();
                    bookValues.put(KEY_BOOK_NAME, bookName);
                    bookValues.put(KEY_BOOK_COVER, bookCover);
                    bookValues.put(KEY_BOOK_THUMB, bookThumb);
                    bookValues.put(KEY_BOOK_POSITION, bookPosition);
                    long newBookId = db.insert(TABLE_BOOKS, null, bookValues);

                    //generate int array from string
                    String[] mediaIDsAsSplittedString = bookMediaContaining.split(",");
                    int[] mediaIDsAsSplittedInt = new int[mediaIDsAsSplittedString.length];
                    for (int i = 0; i < mediaIDsAsSplittedInt.length; i++) {
                        mediaIDsAsSplittedInt[i] = Integer.parseInt(mediaIDsAsSplittedString[i]);
                    }

                    for (int i : mediaIDsAsSplittedInt) {
                        Cursor mediaTableCursor = db.query("tempMediaTable",
                                new String[]{"mediaPath", "mediaName", "mediaPosition", "mediaDuration"},
                                "mediaID = " + i, null, null, null, null);
                        if (mediaTableCursor.moveToFirst()) {
                            String mediaPath = mediaTableCursor.getString(0);
                            String mediaName = mediaTableCursor.getString(1);
                            int mediaPosition = mediaTableCursor.getInt(2);
                            int mediaDuration = mediaTableCursor.getInt(3);

                            //adding these values in new media table
                            ContentValues mediaValues = new ContentValues();
                            mediaValues.put(KEY_MEDIA_PATH, mediaPath);
                            mediaValues.put(KEY_MEDIA_NAME, mediaName);
                            mediaValues.put(KEY_MEDIA_POSITION, mediaPosition);
                            mediaValues.put(KEY_MEDIA_DURATION, mediaDuration);
                            mediaValues.put(KEY_MEDIA_BOOK_ID, newBookId);
                            db.insert(TABLE_MEDIA, null, mediaValues);
                        }
                    }

                }
                //dropping temporary tables
                db.execSQL("DROP TABLE tempMediaTable");
                db.execSQL("DROP TABLE tempBookTable");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }


    public BookDetail getBook(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BOOKS,
                null,
                KEY_BOOK_ID + " = " + id,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            BookDetail book = new BookDetail();
            book.setId(Integer.parseInt(cursor.getString(0)));
            book.setName(cursor.getString(1));
            book.setCover(cursor.getString(2));
            book.setThumb(cursor.getString(3));
            if (cursor.getString(4) != null)
                book.setPosition(Integer.parseInt(cursor.getString(4)));
            cursor.close();
            return book;
        }
        return null;
    }

    /*
    returns the progress of a book in °/°°
     */
    public int getGlobalProgress(int bookId) {
        int duration = 0;
        int progress = 0;
        boolean progressFound = false;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // finds out the position in a book
            Cursor bookCursor = db.query(TABLE_BOOKS,
                    new String[]{KEY_BOOK_POSITION},
                    KEY_BOOK_ID + " = " + bookId,
                    null, null, null, null);
            if (bookCursor != null) {
                if (bookCursor.moveToFirst()) {
                    int bookPosition = bookCursor.getInt(0);
                    bookCursor.close();
                    Cursor mediaCursor = db.query(TABLE_MEDIA,
                            new String[]{KEY_MEDIA_ID, KEY_MEDIA_DURATION, KEY_MEDIA_POSITION},
                            KEY_MEDIA_BOOK_ID + " = " + bookId,
                            null, null, null, null);
                    if (mediaCursor != null) {
                        // adds the sum of length and the sum of played time
                        while (mediaCursor.moveToNext()) {
                            duration += mediaCursor.getInt(1);
                            if (!progressFound) {
                                if (mediaCursor.getInt(0) == bookPosition) {
                                    progress += mediaCursor.getInt(2);
                                    progressFound = true;
                                } else
                                    progress += mediaCursor.getInt(1);
                            }
                        }
                        if (!progressFound)
                            progress = 0;
                        mediaCursor.close();
                    }
                }

            }


            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (BuildConfig.DEBUG) Log.d("dbh", "Found progress: " + progress + "/" + duration);
        if (duration == 0 || progress == 0)
            return 0;
        else {
            return (progress * 1000 / duration);
        }

    }


    public void addMedia(ArrayList<MediaDetail> media) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (MediaDetail m : media) {
                ContentValues values = new ContentValues();
                if (BuildConfig.DEBUG) Log.d("bla", "adding path" + m.getPath());
                values.put(KEY_MEDIA_PATH, m.getPath()); // get title
                values.put(KEY_MEDIA_NAME, m.getName());
                values.put(KEY_MEDIA_POSITION, m.getPosition()); // get author
                values.put(KEY_MEDIA_DURATION, m.getDuration());
                values.put(KEY_MEDIA_BOOK_ID, m.getBookId());
                db.insert(TABLE_MEDIA, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int addBook(BookDetail book) {
        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_THUMB, book.getThumb());
        SQLiteDatabase db = this.getWritableDatabase();
        return (int) db.insert(TABLE_BOOKS, null, values);
    }


    public MediaDetail[] getMediaFromBook(int bookId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MEDIA,
                null,
                KEY_MEDIA_BOOK_ID + " = " + bookId,
                null, null, null,
                KEY_MEDIA_ID);

        if (BuildConfig.DEBUG) Log.d("dbh", "cursor has size:" + cursor.getCount());
        if (BuildConfig.DEBUG) Log.d("dbh", "book id is " + bookId);

        if (cursor != null) {
            MediaDetail[] containingMedia = new MediaDetail[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                MediaDetail media = new MediaDetail();
                media.setId(Integer.parseInt(cursor.getString(0)));
                media.setPath(cursor.getString(1));
                media.setName(cursor.getString(2));
                media.setPosition(Integer.parseInt(cursor.getString(3)));
                media.setDuration(Integer.parseInt(cursor.getString(4)));
                media.setBookId(Integer.parseInt(cursor.getString(5)));
                containingMedia[i++] = media;
            }
            cursor.close();
            return containingMedia;
        }
        return null;
    }


    public ArrayList<BookDetail> getAllBooks() {
        ArrayList<BookDetail> allBooks = new ArrayList<BookDetail>();
        String query = "SELECT  * FROM " + TABLE_BOOKS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        BookDetail book;
        if (cursor.moveToFirst()) {
            do {
                book = new BookDetail();
                book.setId(Integer.parseInt(cursor.getString(0)));
                book.setName(cursor.getString(1));
                book.setCover(cursor.getString(2));
                book.setThumb(cursor.getString(3));
                if (cursor.getString(4) != null)
                    book.setPosition(Integer.parseInt(cursor.getString(4)));
                allBooks.add(book);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return allBooks;
    }

    private void updateMedia(MediaDetail media) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MEDIA_PATH, media.getPath()); // get title
        values.put(KEY_MEDIA_NAME, media.getName());
        values.put(KEY_MEDIA_POSITION, media.getPosition()); // get author
        values.put(KEY_MEDIA_DURATION, media.getDuration());

        db.update(TABLE_MEDIA, values, KEY_MEDIA_ID + " = ?", new String[]{String.valueOf(media.getId())});
    }

    public void updateMediaAsync(MediaDetail media) {
        new AsyncTask<MediaDetail, Void, Void>() {
            @Override
            protected Void doInBackground(MediaDetail... params) {
                updateMedia(params[0]);
                return null;
            }
        }.execute(media);
    }

    private void updateBook(BookDetail book) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_THUMB, book.getThumb());
        values.put(KEY_BOOK_POSITION, book.getPosition());

        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + book.getId(), null);
    }


    public void updateBookAsync(BookDetail book) {
        new AsyncTask<BookDetail, Void, Void>() {
            @Override
            protected Void doInBackground(BookDetail... params) {
                updateBook(params[0]);
                return null;
            }
        }.execute(book);
    }

    public void deleteBook(BookDetail book) {
        SQLiteDatabase db = this.getWritableDatabase();
        int bookId = book.getId();

        db.delete(TABLE_MEDIA,
                KEY_MEDIA_BOOK_ID + " = " + bookId,
                null);
        db.delete(TABLE_BOOKS,
                KEY_BOOK_ID + " = " + bookId,
                null);

        if (book.getCover() != null) {
            File cover = new File(book.getCover());
            //noinspection ResultOfMethodCallIgnored
            cover.delete();
        }

        if (book.getThumb() != null) {
            File thumb = new File(book.getThumb());
            //noinspection ResultOfMethodCallIgnored
            thumb.delete();
        }
    }
}

