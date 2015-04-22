package de.ph1b.audiobook.model;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.utils.Validate;


public class Book implements Comparable<Book> {

    public static final String TAG = Book.class.getSimpleName();
    private static final long ID_UNKNOWN = -1;
    private long id = ID_UNKNOWN;
    @NonNull
    private final String root;
    @NonNull
    private final ArrayList<Chapter> chapters;
    @NonNull
    private final Type type;
    @NonNull
    private final String packageName;
    @NonNull
    private final ArrayList<Bookmark> bookmarks;
    @NonNull
    private String name;
    private int time = 0;
    private float playbackSpeed = 1.0f;
    @NonNull
    private String currentMediaPath;
    private boolean useCoverReplacement = false;

    public Book(@NonNull String root,
                @NonNull String name,
                @NonNull ArrayList<Chapter> chapters,
                @NonNull
                String currentMediaPath,
                @NonNull Type type,
                @NonNull ArrayList<Bookmark> bookmarks,
                @NonNull Context c) {
        new Validate().notNull(root, name, chapters, currentMediaPath, type)
                .notEmpty(root, name)
                .notEmpty(chapters);

        this.root = root;
        this.name = name;
        this.chapters = chapters;
        this.type = type;
        this.bookmarks = bookmarks;
        this.packageName = c.getPackageName();
        setPosition(0, currentMediaPath);
    }

    @NonNull
    public ArrayList<Bookmark> getBookmarks() {
        return bookmarks;
    }


    @NonNull
    public File getCoverFile() {
        File coverFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + packageName,
                id + ".jpg");
        if (!coverFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.getParentFile().mkdirs();
        }
        return coverFile;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public void setPosition(int time, @NonNull String currentMediaPath) {

        boolean relativeMediaPathExists = false;
        for (Chapter c : chapters) {
            if (c.getPath().equals(currentMediaPath)) {
                relativeMediaPathExists = true;
            }
        }
        if (!relativeMediaPathExists) {
            throw new IllegalArgumentException("Creating book with name=" + name +
                    " failed because currentMediaPath=" + currentMediaPath +
                    " does not exist in chapters");
        }

        this.time = time;
        this.currentMediaPath = currentMediaPath;
    }

    public boolean isUseCoverReplacement() {
        return useCoverReplacement;
    }

    public void setUseCoverReplacement(boolean useCoverReplacement) {
        this.useCoverReplacement = useCoverReplacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Book) {
            Book that = (Book) o;

            if (!(this.root.equals(that.root))) return false;

            if (this.chapters.size() != that.chapters.size()) {
                return false;
            } else {
                for (int i = 0; i < this.chapters.size(); i++) {
                    if (!this.chapters.get(i).equals(that.chapters.get(i))) {
                        return false;
                    }
                }
            }

            return this.type == that.type;

        }
        return false;
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

    @Override
    public String toString() {
        return TAG + "[" +
                "root=" + root +
                ", type=" + type +
                ", id=" + id + ", " +
                ", name=" + name +
                ", time=" + time +
                ", playbackSpeed=" + playbackSpeed +
                ", currentMediaPath=" + currentMediaPath +
                ", useCoverReplacement=" + useCoverReplacement +
                ", chapters=" + chapters +
                "]";
    }

    @NonNull
    public String getCurrentMediaPath() {
        return currentMediaPath;
    }

    @Nullable
    public Chapter getNextChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex < chapters.size() - 1) {
            return chapters.get(currentIndex + 1);
        }
        return null;
    }

    @NonNull
    public Chapter getCurrentChapter() {
        for (Chapter c : chapters) {
            if (c.getPath().equals(currentMediaPath)) {
                return c;
            }
        }
        throw new IllegalArgumentException("getCurrentChapter has no valid id with" +
                " currentMediaPath=" + currentMediaPath);
    }

    @Nullable
    public Chapter getPreviousChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex > 0) {
            return chapters.get(currentIndex - 1);
        }
        return null;
    }

    public int getTime() {
        return time;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        new Validate().notNull(name)
                .notEmpty(name);
        this.name = name;
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

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    @NonNull
    public String getRoot() {
        return root;
    }

    @Override
    public int compareTo(@NonNull Book that) {
        return new NaturalOrderComparator().compare(this.name, that.name);
    }

    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }
}

