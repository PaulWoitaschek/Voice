package de.ph1b.audiobook.utils;

import android.os.Build;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Class containing methods for recognizing different file types by their file ending.
 *
 * @author Paul Woitaschek
 */
public class FileRecognition {

    private static final List<String> AUDIO_TYPES = new ArrayList<>(30);
    public static final FileFilter FOLDER_AND_MUSIC_FILTER = pathname -> {
        for (String s : AUDIO_TYPES) {
            if (pathname.getName().toLowerCase().endsWith(s)) {
                return true;
            }
        }
        return pathname.isDirectory();
    };
    private static final List<String> IMAGE_TYPES = Arrays.asList(".jpg", ".jpeg", ".png", ".bmp");
    public static final FileFilter FOLDER_AND_IMAGES_FILTER = pathname -> {
        for (String s : IMAGE_TYPES) {
            if (pathname.getAbsolutePath().toLowerCase().endsWith(s)) {
                return true;
            }
        }
        return pathname.isDirectory();
    };

    static {
        AUDIO_TYPES.add(".3gp");

        AUDIO_TYPES.add(".aac");
        AUDIO_TYPES.add(".awb");

        AUDIO_TYPES.add(".flac");

        AUDIO_TYPES.add(".imy");

        AUDIO_TYPES.add(".m4a");
        AUDIO_TYPES.add(".m4b");
        AUDIO_TYPES.add(".mp4");
        AUDIO_TYPES.add(".mid");
        AUDIO_TYPES.add(".mkv");
        AUDIO_TYPES.add(".mp3");
        AUDIO_TYPES.add(".mp3package");
        AUDIO_TYPES.add(".mxmf");

        AUDIO_TYPES.add(".ogg");
        AUDIO_TYPES.add(".oga");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AUDIO_TYPES.add(".opus");
        }
        AUDIO_TYPES.add(".ota");

        AUDIO_TYPES.add(".rtttl");
        AUDIO_TYPES.add(".rtx");

        AUDIO_TYPES.add(".wav");
        AUDIO_TYPES.add(".wma");

        AUDIO_TYPES.add(".xmf");
    }
}
