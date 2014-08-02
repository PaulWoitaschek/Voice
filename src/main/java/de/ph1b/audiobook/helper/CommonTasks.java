package de.ph1b.audiobook.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.activity.MediaView;
import de.ph1b.audiobook.activity.NoExternalStorage;



public class CommonTasks {

    public void checkExternalStorage(Context c){
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state))
            c.startActivity(new Intent(c, NoExternalStorage.class));
    }

    public void startMediaViewIfExternalStorageAvailable(Context c){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            c.startActivity(new Intent(c, MediaView.class));
    }

    public static void logD(String tag, String message) {
        if (BuildConfig.DEBUG)
            Log.d(tag, message);
    }

}
