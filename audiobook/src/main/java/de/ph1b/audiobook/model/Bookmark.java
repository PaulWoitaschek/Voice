package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;

public class Bookmark {

    private static final String TAG = Bookmark.class.getSimpleName();
    private final int time;
    @NonNull
    private final File mediaFile;
    @NonNull
    private String title;


    public Bookmark(Bookmark that) {
        this.time = that.time;
        this.mediaFile = that.mediaFile;
        this.title = that.title;
    }

    public Bookmark(@NonNull File mediaFile, @NonNull String title, int time) {
        checkArgument(!title.isEmpty());

        this.mediaFile = mediaFile;
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
            return this.time == that.time && this.mediaFile.equals(that.mediaFile) && that.title.equals(this.title);
        }

        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + time;
        result = PRIME * result + mediaFile.hashCode();
        result = PRIME * result + title.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "title=" + title +
                ",time=" + time +
                ",mediaFile=" + mediaFile +
                "]";
    }

    @NonNull
    public File getMediaFile() {
        return mediaFile;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        checkArgument(!title.isEmpty());
        this.title = title;
    }

    public int getTime() {
        return time;
    }

    public ContentValues getContentValues(long bookId) {
        ContentValues cv = new ContentValues();
        cv.put(DataBaseHelper.BOOKMARK_TIME, time);
        cv.put(DataBaseHelper.BOOKMARK_PATH, mediaFile.getAbsolutePath());
        cv.put(DataBaseHelper.BOOKMARK_TITLE, title);
        cv.put(DataBaseHelper.BOOK_ID, bookId);
        return cv;
    }
}
