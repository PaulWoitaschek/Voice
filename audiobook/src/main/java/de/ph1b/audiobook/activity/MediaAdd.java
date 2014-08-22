package de.ph1b.audiobook.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.fragment.ChooseFilesFragment;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.NaturalOrderComparator;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;


public class MediaAdd extends ActionBarActivity {

    private static final String TAG = "de.ph1b.audiobook.activities.MediaAdd";
    public static final String FILES_AS_STRING = TAG + ".FILES_AS_STRING";
    public static final String BOOK_PROPERTIES_DEFAULT_NAME = TAG + ".BOOK_PROPERTIES_DEFAULT_NAME";
    public static final int AUDIO = 1;
    public static final int IMAGE = 2;

    private static ArrayList<String> audioTypes = genAudioTypes();

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

    private static ArrayList<File> endList;


    public static final FileFilter filterShowAudioAndFolder = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden() && (pathname.isDirectory() || isAudio(pathname.getName()));
        }
    };

    private OnBackPressedListener onBackPressedListener;

    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null)
            onBackPressedListener.backPressed();
        else
            super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, new ChooseFilesFragment())
                .addToBackStack(ChooseFilesFragment.TAG)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

    private static boolean isAudio(String name) {
        for (String s : audioTypes)
            if (name.endsWith(s))
                return true;
        return false;
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    public static ArrayList<File> dirsToFiles(FileFilter filter, ArrayList<File> dir, int choice) {
        endList = new ArrayList<File>();
        for (File f : dir) {
            if (choice == AUDIO && isAudio(f.getName())) {
                endList.add(f);
            } else if (choice == IMAGE && isImage(f.getName())) {
                endList.add(f);
            }
            addDirRec(f, filter);
        }
        return endList;
    }

    private static void addDirRec(File file, FileFilter filter) {
        ArrayList<File> returnList = new ArrayList<File>();
        if (file.isDirectory()) {
            File[] tempList = file.listFiles(filter);

            if (tempList != null) {
                Collections.sort(Arrays.asList(tempList), new NaturalOrderComparator<File>());
                for (File f : tempList) {
                    if (f.isDirectory()) {
                        addDirRec(f, filter);
                    }
                }
                for (File f : tempList) {
                    if (!f.isDirectory())
                        returnList.add(f);
                }
            }
        }
        Collections.sort(returnList, new NaturalOrderComparator<File>());
        for (File f : returnList)
            if (BuildConfig.DEBUG)
                Log.d(TAG, f.getAbsolutePath());
        endList.addAll(returnList);
    }

    public static boolean isImage(String s) {
        return s.endsWith(".jpg") || s.endsWith(".png");
    }
}
