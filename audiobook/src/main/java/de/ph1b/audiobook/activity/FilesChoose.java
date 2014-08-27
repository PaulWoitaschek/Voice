package de.ph1b.audiobook.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;
import de.ph1b.audiobook.utils.CommonTasks;


public class FilesChoose extends ActionBarActivity {

    private static final String TAG = "de.ph1b.audiobook.activities.MediaAdd";
    public static final String FILES_AS_STRING = TAG + ".FILES_AS_STRING";
    public static final String BOOK_PROPERTIES_DEFAULT_NAME = TAG + ".BOOK_PROPERTIES_DEFAULT_NAME";

    private static final ArrayList<String> audioTypes = genAudioTypes();

    private static ArrayList<String> genAudioTypes() {
        ArrayList<String> audioTypes = new ArrayList<String>();
        audioTypes.add(".3gp");
        audioTypes.add(".mp4");
        audioTypes.add(".m4a");
        audioTypes.add(".mp3");
        audioTypes.add(".mid");
        audioTypes.add(".xmf");
        audioTypes.add(".mxmf");
        audioTypes.add(".rtttl");
        audioTypes.add(".rtx");
        audioTypes.add(".ota");
        audioTypes.add(".imy");
        audioTypes.add(".ogg");
        audioTypes.add(".wav");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            audioTypes.add(".aac");
            audioTypes.add(".flac");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            audioTypes.add(".mkv");

        return audioTypes;
    }


    private OnBackPressedListener onBackPressedListener;

    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null)
            onBackPressedListener.backPressed();
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_choose);
    }

    public static boolean isAudio(String name) {
        for (String s : audioTypes)
            if (name.endsWith(s))
                return true;
        return false;
    }

    public static boolean isImage(String s) {
        return s.endsWith(".jpg") || s.endsWith(".png");
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    @Override
    public void onResume() {
        CommonTasks.checkExternalStorage(this);
        super.onResume();
    }
}
