package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import de.ph1b.audiobook.utils.Validate;

public class Bookmark {

    private static final String TAG = Bookmark.class.getSimpleName();
    private final int time;
    @NonNull
    private final String mediaPath;
    @NonNull
    private String title;


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
                ",title=" + title +
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
}
