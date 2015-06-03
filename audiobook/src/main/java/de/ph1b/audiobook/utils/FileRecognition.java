package de.ph1b.audiobook.utils;

import android.os.Build;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;


/**
 * Class containing methods for recognizing different file types by their file ending.
 */
public class FileRecognition {

    private static final ArrayList<String> audioTypes = new ArrayList<>();
    /**
     * Recognizing supported audio files
     * {@inheritDoc}
     */
    public static final FileFilter audioFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            for (String s : audioTypes) {
                if (f.getName().toLowerCase().endsWith(s)) {
                    return true;
                }
            }
            return false;
        }
    };
    public static final FileFilter folderAndMusicFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return audioFilter.accept(pathname) || pathname.isDirectory();
        }
    };
    private static final ArrayList<String> imageTypes = new ArrayList<>();
    /**
     * Recognizing supported image types.
     * {@inheritDoc}
     */
    public static final FileFilter imageFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            for (String s : imageTypes) {
                if (pathname.getAbsolutePath().toLowerCase().endsWith(s))
                    return true;
            }
            return false;
        }
    };


    static {
        audioTypes.add(".3gp");

        audioTypes.add(".aac");
        audioTypes.add(".awb");

        audioTypes.add(".flac");

        audioTypes.add(".imy");

        audioTypes.add(".m4a");
        audioTypes.add(".m4b");
        audioTypes.add(".mp4");
        audioTypes.add(".mid");
        audioTypes.add(".mkv");
        audioTypes.add(".mp3");
        audioTypes.add(".mxmf");

        audioTypes.add(".ogg");
        audioTypes.add(".oga");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) audioTypes.add(".opus");
        audioTypes.add(".ota");

        audioTypes.add(".rtttl");
        audioTypes.add(".rtx");

        audioTypes.add(".wav");
        audioTypes.add(".wma");

        audioTypes.add(".xmf");

        imageTypes.add(".jpg");
        imageTypes.add(".jpeg");
        imageTypes.add(".bmp");
        imageTypes.add(".png");
    }
}
