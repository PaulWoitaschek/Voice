package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import de.ph1b.audiobook.utils.Validate;

public class Bookmark {

    private static final String TAG = Bookmark.class.getSimpleName();
    private final int time;
    @NonNull
    private final String mediaPath;
    @NonNull
    private String title;


    public Bookmark(Bookmark that) {
        this.time = that.time;
        this.mediaPath = that.mediaPath;
        this.title = that.title;
    }

    public Bookmark(@NonNull String mediaPath, @NonNull String title, int time) {
        new Validate().notNull(mediaPath, title)
                .notEmpty(mediaPath, title);

        this.mediaPath = mediaPath;
        this.title = title;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Bookmark) {
            Bookmark that = (Bookmark) o;
            return this.time == that.time && this.mediaPath.equals(that.mediaPath) && that.title.equals(this.title);
        }

        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + time;
        result = PRIME * result + mediaPath.hashCode();
        result = PRIME * result + title.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "title=" + title +
                ",time=" + time +
                ",mediaPath=" + mediaPath +
                "]";
    }

    @NonNull
    public String getMediaPath() {
        return mediaPath;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        new Validate().notNull(title)
                .notEmpty(title);
        this.title = title;
    }

    public int getTime() {
        return time;
    }

    public ContentValues getContentValues(long bookId) {
        ContentValues cv = new ContentValues();
        cv.put(DataBaseHelper.BOOKMARK_TIME, time);
        cv.put(DataBaseHelper.BOOKMARK_PATH, mediaPath);
        cv.put(DataBaseHelper.BOOKMARK_TITLE, title);
        cv.put(DataBaseHelper.BOOK_ID, bookId);
        return cv;
    }
}
