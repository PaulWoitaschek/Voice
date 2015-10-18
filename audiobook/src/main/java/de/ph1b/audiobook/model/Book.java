package de.ph1b.audiobook.model;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.ph1b.audiobook.interfaces.GenericBuilder;
import de.ph1b.audiobook.utils.App;


public class Book implements Comparable<Book> {

    public static final String TAG = Book.class.getSimpleName();
    public static final long ID_UNKNOWN = -1;
    private static final String COVER_TRANSITION_PREFIX = "bookCoverTransition_";
    @NonNull private final String root;
    @NonNull private final List<Chapter> chapters;
    @NonNull private final Type type;
    @NonNull private final List<Bookmark> bookmarks;
    @Nullable private final String author;
    private final boolean useCoverReplacement;
    @NonNull private final String name;
    @Inject Context c;
    private long id = ID_UNKNOWN;
    private int time = 0;
    private float playbackSpeed = 1.0f;
    @NonNull private File currentFile;

    private Book(Builder builder) {
        App.getComponent().inject(this);
        this.root = builder.root;
        this.chapters = new ArrayList<>(builder.chapters);
        this.type = builder.type;
        this.bookmarks = new ArrayList<>(builder.bookmarks);
        this.author = builder.author;
        this.name = builder.name;
        this.currentFile = builder.currentFile;
        this.useCoverReplacement = builder.useCoverReplacement;
        this.playbackSpeed = builder.playbackSpeed;
        this.id = builder.id;
        this.time = builder.time;
    }

    /**
     * Gets the transition name for the cover transition.
     *
     * @return The transition name
     */
    @NonNull
    public String getCoverTransitionName() {
        return COVER_TRANSITION_PREFIX + id;
    }

    @NonNull
    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    /**
     * @return The global duration. It sums up the duration of all chapters.
     */
    public int getGlobalDuration() {
        int globalDuration = 0;
        for (Chapter c : chapters) {
            globalDuration += c.duration();
        }
        return globalDuration;
    }

    /**
     * @return The global position. It sums up the duration of all elapsed chapters plus the position
     * in the current chapter.
     */
    public int getGlobalPosition() {
        int globalPosition = 0;
        for (Chapter c : chapters) {
            if (c.equals(getCurrentChapter())) {
                globalPosition += time;
                return globalPosition;
            } else {
                globalPosition += c.duration();
            }
        }
        throw new IllegalStateException("Current chapter was not found while looking up the global position");
    }

    @NonNull
    public File getCoverFile() {
        File coverFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + c.getPackageName(),
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

    public void setPosition(int time, @NonNull File currentFile) {
        boolean relativeMediaPathExists = false;
        for (Chapter c : chapters) {
            if (c.file().equals(currentFile)) {
                relativeMediaPathExists = true;
            }
        }
        if (!relativeMediaPathExists) {
            throw new IllegalArgumentException("Creating book with name=" + name +
                    " failed because currentFile=" + currentFile +
                    " does not exist in chapters=" + chapters);
        }

        this.time = time;
        this.currentFile = currentFile;
    }

    public boolean isUseCoverReplacement() {
        return useCoverReplacement;
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
                    this.type == that.type &&
                    this.name.equals(that.name);

        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME;
        result = PRIME * result + root.hashCode();
        result = PRIME * result + chapters.hashCode();
        result = PRIME * result + name.hashCode();
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
                ", currentFile=" + currentFile +
                ", useCoverReplacement=" + useCoverReplacement +
                ", chapters=" + chapters +
                ", bookmarks=" + bookmarks +
                "]";
    }

    @NonNull
    public File getCurrentFile() {
        return currentFile;
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
            if (c.file().equals(currentFile)) {
                return c;
            }
        }
        throw new IllegalArgumentException("getCurrentChapter has no valid id with" +
                " currentFile=" + currentFile);
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
        if (this.equals(that)) {
            return 0;
        } else {
            return NaturalOrderComparator.naturalCompare(this.name, that.name);
        }
    }

    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }

    public static class Builder implements GenericBuilder<Book> {

        @NonNull private final String root;
        @NonNull private final List<Chapter> chapters;
        @NonNull private final Type type;
        @NonNull private final List<Bookmark> bookmarks;
        @Nullable private final String author;
        private long id = ID_UNKNOWN;
        private int time = 0;
        @NonNull private File currentFile;
        private boolean useCoverReplacement;
        @NonNull private String name;
        private float playbackSpeed = 1.0f;

        public Builder(Book book) {
            this.root = book.root;
            this.chapters = book.chapters;
            this.type = book.type;
            this.bookmarks = book.bookmarks;
            this.author = book.author;

            this.id = book.id;
            this.time = book.time;
            this.playbackSpeed = book.playbackSpeed;
            this.currentFile = book.currentFile;
            this.useCoverReplacement = book.useCoverReplacement;
            this.name = book.name;
        }

        public Builder(@NonNull String root, @NonNull List<Chapter> chapters, @NonNull Type type,
                       @NonNull List<Bookmark> bookmarks, @Nullable String author, @NonNull File currentFile,
                       @NonNull String name, boolean useCoverReplacement) {
            this.root = root;
            this.chapters = chapters;
            this.type = type;
            this.bookmarks = bookmarks;
            this.author = author;
            this.name = name;
            this.currentFile = currentFile;
            this.useCoverReplacement = useCoverReplacement;
        }

        public Builder useCoverReplacement(boolean useCoverReplacement) {
            this.useCoverReplacement = useCoverReplacement;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public Book build() {
            return new Book(this);
        }
    }
}

