package de.ph1b.audiobook.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "audioBookDB";
    private static final int DATABASE_VERSION = 1;

    // Books table name
    private static final String TABLE_MEDIA = "mediaTable";
    private static final String TABLE_BOOKS = "bookTable";

    // Books Table Columns names
    private static final String KEY_MEDIA_ID = "mediaID";
    private static final String KEY_MEDIA_PATH = "mediaPath";
    private static final String KEY_MEDIA_NAME = "mediaName";
    private static final String KEY_MEDIA_POSITION = "mediaPosition";
    private static final String KEY_MEDIA_DURATION = "mediaDuration";

    private static final String KEY_BOOK_ID = "bookID";
    private static final String KEY_BOOK_NAME = "bookName";
    private static final String KEY_BOOK_COVER = "bookCover";
    private static final String KEY_BOOK_CONTAINING = "bookMediaContaining";
    private static final String KEY_BOOK_POSITION = "bookPosition";
    private static final String KEY_BOOK_THUMB = "bookThumb";

    private static final String[] COLUMNS_MEDIA = {KEY_MEDIA_ID, KEY_MEDIA_PATH, KEY_MEDIA_NAME, KEY_MEDIA_POSITION, KEY_MEDIA_DURATION};
    private static final String[] COLUMNS_BOOKS = {KEY_BOOK_ID, KEY_BOOK_NAME, KEY_BOOK_COVER, KEY_BOOK_THUMB, KEY_BOOK_CONTAINING, KEY_BOOK_POSITION};

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
                    KEY_MEDIA_DURATION + " INTEGER"
                    + ")";

            db.execSQL(CREATE_MEDIA_TABLE);

            String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " ( " +
                    KEY_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_BOOK_NAME + " TEXT, " +
                    KEY_BOOK_COVER + " TEXT, " +
                    KEY_BOOK_THUMB + " TEXT, " +
                    KEY_BOOK_CONTAINING + " TEXT, " +
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
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKS);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        this.onCreate(db);
    }

    public MediaDetail getMedia(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MEDIA, COLUMNS_MEDIA, " " + KEY_MEDIA_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        MediaDetail media = new MediaDetail();
        if (cursor != null) {
            media.setId(Integer.parseInt(cursor.getString(0)));
            media.setPath(cursor.getString(1));
            media.setName(cursor.getString(2));
            media.setPosition(Integer.parseInt(cursor.getString(3)));
            media.setDuration(Integer.parseInt(cursor.getString(4)));
            cursor.close();
        }
        return media;
    }

    public BookDetail getBook(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BOOKS, COLUMNS_BOOKS, " " + KEY_BOOK_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        BookDetail book = new BookDetail();
        if (cursor != null && cursor.getCount() > 0) {
            book.setId(Integer.parseInt(cursor.getString(0)));
            book.setName(cursor.getString(1));
            book.setCover(cursor.getString(2));
            book.setThumb(cursor.getString(3));
            book.setMediaIDs(cursor.getString(4));
            if (cursor.getString(5) != null)
                book.setPosition(Integer.parseInt(cursor.getString(5)));
            cursor.close();
            return book;
        } else
            return null;
    }

    //returns bookId of added media
    public int addMedia(MediaDetail media) {
        ContentValues values = new ContentValues();
        values.put(KEY_MEDIA_PATH, media.getPath()); // get title
        values.put(KEY_MEDIA_NAME, media.getName());
        values.put(KEY_MEDIA_POSITION, media.getPosition()); // get author
        values.put(KEY_MEDIA_DURATION, media.getDuration());
        SQLiteDatabase db = this.getWritableDatabase();
        return (int) db.insert(TABLE_MEDIA, null, values);
    }

    public void addBook(BookDetail book) {
        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_THUMB, book.getThumb());
        values.put(KEY_BOOK_CONTAINING, book.getMediaIdsAsString());
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_BOOKS, null, values);
    }


    public MediaDetail[] getMediaFromBook(int bookId) {
        SQLiteDatabase db = this.getReadableDatabase();
        BookDetail book = getBook(bookId);
        int[] containingId = book.getMediaIds();
        MediaDetail[] containingMedia = new MediaDetail[containingId.length];

        db.beginTransaction();
        try {
            for (int i = 0; i < containingMedia.length; i++) {
                Cursor cursor = db.query(TABLE_MEDIA, COLUMNS_MEDIA, " " + KEY_MEDIA_ID + " = ?", new String[]{String.valueOf(containingId[i])}, null, null, null, null);
                if (cursor != null)
                    cursor.moveToFirst();
                MediaDetail media = new MediaDetail();
                if (cursor != null) {
                    media.setId(Integer.parseInt(cursor.getString(0)));
                    media.setPath(cursor.getString(1));
                    media.setName(cursor.getString(2));
                    media.setPosition(Integer.parseInt(cursor.getString(3)));
                    media.setDuration(Integer.parseInt(cursor.getString(4)));
                    cursor.close();
                }
                containingMedia[i] = media;
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return containingMedia;
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
                book.setMediaIDs(cursor.getString(4));
                if (cursor.getString(5) != null)
                    book.setPosition(Integer.parseInt(cursor.getString(5)));
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
        values.put(KEY_BOOK_CONTAINING, book.getMediaIdsAsString());
        values.put(KEY_BOOK_POSITION, book.getPosition());

        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = ?", new String[]{String.valueOf(book.getId())});
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
        int[] mediaIds = book.getMediaIds();

        db.beginTransaction();
        try {
            db.delete(TABLE_BOOKS, KEY_BOOK_ID + " = ?", new String[]{String.valueOf(bookId)});
            for (int mediaId : mediaIds) {
                db.delete(TABLE_MEDIA, KEY_MEDIA_ID + " = ?", new String[]{String.valueOf(mediaId)});
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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

