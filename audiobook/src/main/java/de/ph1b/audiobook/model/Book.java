package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class Book implements Comparable<Book> {

    public static final String TAG = Book.class.getSimpleName();
    public static final long ID_UNKNOWN = -1;
    @NonNull
    private final String root;
    @NonNull
    private final List<Chapter> chapters;
    @NonNull
    private final Type type;
    @NonNull
    private final String packageName;
    @NonNull
    private final List<Bookmark> bookmarks;
    @Nullable
    private final String author;
    private long id = ID_UNKNOWN;
    @NonNull
    private String name;
    private int time = 0;
    private float playbackSpeed = 1.0f;
    @NonNull
    private String currentMediaPath;
    private boolean useCoverReplacement = false;

    public Book(Book that) {
        this.id = that.id;
        this.root = that.root;
        List<Chapter> copyChapters = new ArrayList<>();
        for (Chapter c : that.chapters) {
            copyChapters.add(new Chapter(c));
        }
        this.chapters = copyChapters;
        this.type = Type.valueOf(that.type.name());
        this.packageName = that.packageName;
        List<Bookmark> copyBookmarks = new ArrayList<>();
        for (Bookmark b : that.bookmarks) {
            copyBookmarks.add(new Bookmark(b));
        }
        this.bookmarks = copyBookmarks;
        this.name = that.name;
        this.author = that.author;
        this.time = that.time;
        this.playbackSpeed = that.playbackSpeed;
        this.currentMediaPath = that.currentMediaPath;
        this.useCoverReplacement = that.useCoverReplacement;
    }


    public Book(@NonNull String root,
                @NonNull String name,
                @Nullable String author,
                @NonNull List<Chapter> chapters,
                @NonNull String currentMediaPath,
                @NonNull Type type,
                @NonNull List<Bookmark> bookmarks,
                @NonNull Context c) {
        checkNotNull(chapters);
        checkNotNull(currentMediaPath);
        checkNotNull(type);
        checkNotNull(bookmarks);
        checkArgument(!root.isEmpty());
        checkArgument(!name.isEmpty());
        checkArgument(!chapters.isEmpty());

        this.root = root;
        this.name = name;
        this.author = author;
        this.chapters = chapters;
        this.type = type;
        this.bookmarks = bookmarks;
        this.packageName = c.getPackageName();
        setPosition(0, currentMediaPath);
        this.currentMediaPath = currentMediaPath;
    }

    public ContentValues getContentValues() {
        ContentValues bookCv = new ContentValues();
        bookCv.put(DataBaseHelper.BOOK_NAME, name);
        bookCv.put(DataBaseHelper.BOOK_AUTHOR, author);
        bookCv.put(DataBaseHelper.BOOK_ACTIVE, 1);
        bookCv.put(DataBaseHelper.BOOK_CURRENT_MEDIA_PATH, currentMediaPath);
        bookCv.put(DataBaseHelper.BOOK_PLAYBACK_SPEED, playbackSpeed);
        bookCv.put(DataBaseHelper.BOOK_ROOT, root);
        bookCv.put(DataBaseHelper.BOOK_TIME, time);
        bookCv.put(DataBaseHelper.BOOK_TYPE, type.name());
        bookCv.put(DataBaseHelper.BOOK_USE_COVER_REPLACEMENT, useCoverReplacement);
        return bookCv;
    }

    @NonNull
    public List<Bookmark> getBookmarks() {
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


    /**
     * @return the author of the book or null if not set.
     */
    @Nullable
    public String getAuthor() {
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Book) {
            Book that = (Book) o;

            return this.root.equals(that.root) &&
                    this.chapters.equals(that.chapters) &&
                    this.type == that.type;

        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME;
        result = PRIME * result + root.hashCode();
        result = PRIME * result + chapters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "root=" + root +
                ", type=" + type +
                ", id=" + id +
                ", name=" + name +
                ", author=" + author +
                ", time=" + time +
                ", playbackSpeed=" + playbackSpeed +
                ", currentMediaPath=" + currentMediaPath +
                ", useCoverReplacement=" + useCoverReplacement +
                ", packageName=" + packageName +
                ", chapters=" + chapters +
                ", bookmarks=" + bookmarks +
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
        checkArgument(!name.isEmpty());
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public List<Chapter> getChapters() {
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
        return NaturalOrderComparator.INSTANCE.compare(this.name, that.name);
    }

    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }
}

