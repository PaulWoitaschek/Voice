package de.ph1b.audiobook.utils;

import android.media.MediaMetadataRetriever;

import de.ph1b.audiobook.content.Media;



public class MusicUtil {

    private static final String TAG = "MusicUtil";

    public static void fillMissingDuration(Media media) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        try {
            try {
                if (media.getDuration() == 0) {
                    metaRetriever.setDataSource(media.getPath());
                    int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    media.setDuration(duration);
                }
            } catch (RuntimeException e) { //undocumented exception
                L.e(TAG, "Error at retrieving duration from file=" + media.getPath(), e);
            }
        } finally {
            metaRetriever.release();
        }
    }
}
