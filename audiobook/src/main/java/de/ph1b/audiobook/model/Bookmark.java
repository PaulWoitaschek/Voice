package de.ph1b.audiobook.model;

import de.ph1b.audiobook.utils.ArgumentValidator;

public class Bookmark {

    private static final String TAG = Bookmark.class.getSimpleName();
    private final int time;
    private final String path;
    private String title;

    public Bookmark(String path, String title, int time) {
        ArgumentValidator.validate(path, title);
        this.title = title;
        this.time = time;
        this.path = path;
    }

    public Bookmark(Bookmark bookmark) {
        this(bookmark.path, bookmark.title, bookmark.time);
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

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "title=" + title + ", " +
                "time=" + time + ", " +
                "path=" + path +
                "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("title must not be null");
        }
        this.title = title;
    }

    public int getTime() {
        return time;
    }
}
