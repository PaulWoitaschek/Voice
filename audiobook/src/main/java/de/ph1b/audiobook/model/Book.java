package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.utils.ArgumentValidator;

public class Book implements Comparable<Book> {

    private static final String TAG = Book.class.getSimpleName();

    private final String root;
    @NonNull
    private final ArrayList<Chapter> chapters;
    @NonNull
    private final ArrayList<Bookmark> bookmarks;
    private long id;
    private long sortId = -1;
    @NonNull
    private String name;
    private int time = 0;
    private float playbackSpeed = 1;
    @NonNull
    private String relativeMediaPath;

    public Book(@NonNull String root, @NonNull String name, @NonNull ArrayList<Chapter> chapters, @NonNull ArrayList<Bookmark> bookmarks, float playbackSpeed) {
        ArgumentValidator.validate(root, name, chapters, bookmarks);
        if (name.equals("")) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (chapters.size() == 0) {
            throw new IllegalArgumentException("Book must have any containing chapters.");
        }
        this.root = root;
        this.name = name;
        this.chapters = chapters;


        //check if bookmark exists
        for (Bookmark b : bookmarks) {
            boolean bookmarkExists = false;
            for (Chapter c : chapters) {
                if (b.getPath().equals(c.getPath())) {
                    bookmarkExists = true;
                    break;
                }
            }
            if (!bookmarkExists) {
                throw new IllegalArgumentException("Cannot add bookmark=" + b + " because it is not in chapters=" + chapters);
            }
        }
        this.bookmarks = bookmarks;
        this.playbackSpeed = playbackSpeed;
    }

    public Book(@NonNull String root, @NonNull String name, @NonNull ArrayList<Chapter> chapters, @NonNull ArrayList<Bookmark> bookmarks, float playbackSpeed, long id, long sortId, int time, @NonNull String relativeMediaPath) {
        this(root, name, chapters, bookmarks, playbackSpeed);
        this.id = id;
        this.sortId = sortId;
        ArgumentValidator.validate(relativeMediaPath);
        setPosition(time, relativeMediaPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Book) {
            Book that = (Book) o;

            boolean rootE = this.root.equals(that.root);
            boolean chaptersE = true;

            if (this.chapters.size() != that.chapters.size()) {
                chaptersE = false;
            } else {
                for (int i = 0; i < this.chapters.size(); i++) {
                    if (!this.chapters.get(i).equals(that.chapters.get(i))) {
                        chaptersE = false;
                        break;
                    }
                }
            }

            return (rootE && chaptersE);

        }
        return false;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "root=" + root +
                ",chapters=" + chapters +
                ",bookmarks=" + bookmarks +
                ",id=" + id + ", " +
                ",sortId=" + sortId +
                ",name=" + name +
                ",time=" + time +
                ",playbackSpeed=" + playbackSpeed +
                ",relativeMediaPath=" + relativeMediaPath +
                "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + root.hashCode();
        for (Chapter c : chapters) {
            result = PRIME * result + c.hashCode();
        }
        return result;
    }

    @NonNull
    public String getRelativeMediaPath() {
        return relativeMediaPath;
    }

    public void setPosition(int time, String relativeMediaPath) {
        boolean relativeMediaPathExists = false;
        for (Chapter c : chapters) {
            if (c.getPath().equals(relativeMediaPath)) {
                relativeMediaPathExists = true;
            }
        }
        if (!relativeMediaPathExists) {
            throw new IllegalArgumentException("Creating book with name=" + name + " failed because relativeMediaPath=" + relativeMediaPath + " does not exist in chapters");
        }

        this.relativeMediaPath = relativeMediaPath;
        this.time = time;
        this.relativeMediaPath = relativeMediaPath;
    }

    @Nullable
    public Chapter getNextChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex < chapters.size() - 1) {
            return chapters.get(currentIndex + 1);
        }
        return null;
    }

    @Nullable
    public Chapter getPreviousChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex > 0) {
            return chapters.get(currentIndex - 1);
        }
        return null;
    }

    public Chapter getCurrentChapter() {
        for (Chapter c : chapters) {
            if (c.getPath().equals(relativeMediaPath)) {
                return c;
            }
        }
        throw new IllegalArgumentException("getCurrentChapter has no valid path with relativeMediaPath=" + relativeMediaPath);
    }

    public int getTime() {
        return time;
    }

    public long getSortId() {
        return sortId;
    }

    public void setSortId(long sortId) {
        this.sortId = sortId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        if (name.equals("")) {
            throw new NullPointerException("name must not be empty");
        }
        this.name = name;
    }

    @Nullable
    public File getCoverFile() {
        File coverFile = new File(root, "." + name + ".jpg");
        if (coverFile.exists() && coverFile.canRead()) {
            return coverFile;
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public ArrayList<Chapter> getChapters() {
        return chapters;
    }

    @NonNull
    public ArrayList<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public String getRoot() {
        return root;
    }

    @Override
    public int compareTo(@NonNull Book that) {
        if (this.sortId > that.sortId) {
            return 1;
        } else if (this.sortId < that.sortId) {
            return -1;
        } else {
            return 0;
        }
    }
}

