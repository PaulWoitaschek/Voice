package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.fragment.NoExternalStorageFragment;


public class CommonTasks {

    public void checkExternalStorage(Context c){
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state))
            c.startActivity(new Intent(c, NoExternalStorageFragment.class));
    }

    public void startMediaViewIfExternalStorageAvailable(Context c){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            c.startActivity(new Intent(c, BookChoose.class));
    }
}
