package de.ph1b.audiobook.model;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;

import auto.parcel.AutoParcel;
import de.ph1b.audiobook.utils.App;


@AutoParcel
public abstract class Book implements Comparable<Book> {

    public static final String TAG = Book.class.getSimpleName();
    public static final long ID_UNKNOWN = -1;
    private static final String COVER_TRANSITION_PREFIX = "bookCoverTransition_";

    public static Builder builder(@NonNull String root, @NonNull List<Chapter> chapters, @NonNull Type type,
                                  @NonNull List<Bookmark> bookmarks, @Nullable String author, @NonNull File currentFile,
                                  @NonNull String name, boolean useCoverReplacement, float playbackSpeed) {
        return new AutoParcel_Book.Builder()
                .root(root)
                .chapters(chapters)
                .type(type)
                .bookmarks(bookmarks)
                .author(author)
                .currentFile(currentFile)
                .name(name)
                .useCoverReplacement(useCoverReplacement)
                .id(ID_UNKNOWN)
                .playbackSpeed(playbackSpeed);
    }

    public static Builder builder(Book book) {
        return new AutoParcel_Book.Builder(book);
    }

    /**
     * Gets the transition name for the cover transition.
     *
     * @return The transition name
     */
    @NonNull
    public String coverTransitionName() {
        return COVER_TRANSITION_PREFIX + id();
    }

    @NonNull
    public abstract List<Bookmark> bookmarks();

    /**
     * @return The global duration. It sums up the duration of all chapters.
     */
    public int globalDuration() {
        int globalDuration = 0;
        for (Chapter c : chapters()) {
            globalDuration += c.duration();
        }
        return globalDuration;
    }

    /**
     * @return The global position. It sums up the duration of all elapsed chapters plus the position
     * in the current chapter.
     */
    public int globalPosition() {
        int globalPosition = 0;
        for (Chapter c : chapters()) {
            if (c.equals(currentChapter())) {
                globalPosition += time();
                return globalPosition;
            } else {
                globalPosition += c.duration();
            }
        }
        throw new IllegalStateException("Current chapter was not found while looking up the global position");
    }

    @NonNull
    public File coverFile() {
        File coverFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + App.getComponent().getContext().getPackageName(),
                id() + ".jpg");
        if (!coverFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.getParentFile().mkdirs();
        }
        return coverFile;
    }

    @NonNull
    public abstract Type type();

    public abstract boolean useCoverReplacement();

    /**
     * @return the author of the book or null if not set.
     */
    @Nullable
    public abstract String author();

    @NonNull
    public abstract File currentFile();

    @Nullable
    public Chapter nextChapter() {
        List<Chapter> chapters = chapters();
        int currentIndex = chapters.indexOf(currentChapter());
        if (currentIndex < chapters.size() - 1) {
            return chapters.get(currentIndex + 1);
        }
        return null;
    }

    @NonNull
    public Chapter currentChapter() {
        List<Chapter> chapters = chapters();
        for (Chapter c : chapters) {
            if (c.file().equals(currentFile())) {
                return c;
            }
        }
        throw new IllegalArgumentException("currentChapter has no valid id with" +
                " currentFile=" + currentFile());
    }

    @Nullable
    public Chapter previousChapter() {
        List<Chapter> chapters = chapters();
        int currentIndex = chapters.indexOf(currentChapter());
        if (currentIndex > 0) {
            return chapters.get(currentIndex - 1);
        }
        return null;
    }

    public abstract int time();

    @NonNull
    public abstract String name();

    public abstract long id();

    @NonNull
    public abstract List<Chapter> chapters();

    public abstract float playbackSpeed();

    @NonNull
    public abstract String root();

    @Override
    public int compareTo(@NonNull Book that) {
        if (this.equals(that)) {
            return 0;
        } else {
            return NaturalOrderComparator.naturalCompare(this.name(), that.name());
        }
    }

    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }

    @AutoParcel.Builder
    public abstract static class Builder {

        public abstract Builder currentFile(File currentFile);

        public abstract Builder useCoverReplacement(boolean useCoverReplacement);

        public abstract Builder name(String name);

        public abstract Builder bookmarks(List<Bookmark> bookmarks);

        public abstract Builder chapters(List<Chapter> chapters);

        public abstract Builder root(String root);

        public abstract Builder type(Type type);

        public abstract Builder playbackSpeed(float playbackSpeed);

        public abstract Builder id(long id);

        public abstract Builder time(int time);

        public abstract Book build();

        public abstract Builder author(String author);
    }
}

