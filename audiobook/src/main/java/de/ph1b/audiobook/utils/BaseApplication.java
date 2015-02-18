package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;


public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler {


    private static final String TAG = BaseApplication.class.getSimpleName();
    private final Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ArrayList<Book> allBooks;
    private DataBaseHelper db;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        db = DataBaseHelper.getInstance(this);
        allBooks = db.getAllBooks();

        fillMissingCovers(allBooks);
    }

    public ArrayList<Book> getAllBooks() {
        return allBooks;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        String stackTrace = Log.getStackTraceString(ex);
        String time = new Date(System.currentTimeMillis()).toString();
        String message = ex.getMessage();

        String report = "occured_at\n" + time + "\n\n" +
                "message\n" + message + "\n\n" +
                "stacktrace\n" + stackTrace;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "woitaschek@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bugreport");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report);
        Intent startClientIntent = Intent.createChooser(emailIntent, "Sending mail...");
        startClientIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(startClientIntent);

        defaultUEH.uncaughtException(thread, ex);
    }

    public void fillMissingCovers(final ArrayList<Book> allBooks) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (Book b : allBooks) {
                    String coverPath = b.getCover();
                    if (coverPath == null || !new File(coverPath).exists()) {
                        Bitmap cover = ImageHelper.genCapital(b.getName(), BaseApplication.this);
                        coverPath = ImageHelper.saveCover(cover, BaseApplication.this);
                        b.setCover(coverPath);
                        db.updateBook(b);
                        L.d(TAG, "updated cover from book=" + b);
                    }
                }
            }
        });
    }
}