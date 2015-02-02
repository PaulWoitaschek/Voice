package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Date;

import de.ph1b.audiobook.BuildConfig;


public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler {


    private Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
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
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bugreport Marinamap");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report);
        Intent startClientIntent = Intent.createChooser(emailIntent, "Sending mail...");
        startClientIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(startClientIntent);

        defaultUEH.uncaughtException(thread, ex);
    }
}