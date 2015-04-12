package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import de.ph1b.audiobook.utils.Validate;

public class Bookmark {

    private static final String TAG = Bookmark.class.getSimpleName();
    private final int time;
    @NonNull
    private final String path;
    @NonNull
    private String title;

    public Bookmark(@NonNull String path, @NonNull String title, int time) {
        new Validate().notNull(path, title)
                .notEmpty(path, title);

        this.path = path;
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
            boolean timeE = that.time == this.time;
            boolean pathE = that.path.equals(this.path);
            boolean titleE = that.title.equals(this.title);
            return timeE && pathE && titleE;
        }

        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + time;
        result = PRIME * result + path.hashCode();
        result = PRIME * result + title.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                ",title=" + title +
                ",time=" + time +
                ",path=" + path +
                "]";
    }

    @NonNull
    public String getPath() {
        return path;
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
