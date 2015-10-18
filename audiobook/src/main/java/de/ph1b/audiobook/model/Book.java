package de.ph1b.audiobook.model;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import de.ph1b.audiobook.utils.App;


@AutoParcel
public abstract class Book implements Comparable<Book> {

    public static final String TAG = Book.class.getSimpleName();
    public static final long ID_UNKNOWN = -1;
    public static final float SPEED_MIN = 0.5f;
    public static final float SPEED_MAX = 2f;
    private static final String COVER_TRANSITION_PREFIX = "bookCoverTransition_";

    public static Builder builder(@NonNull String root, @NonNull List<Chapter> chapters, @NonNull Type type,
                                  @NonNull List<Bookmark> bookmarks, @Nullable String author, @NonNull File currentFile,
                                  @NonNull String name, boolean useCoverReplacement, float playbackSpeed) {
        return new AutoParcel_Book.Builder()
                .root(root)
                .chapters(ImmutableList.copyOf(chapters))
                .type(type)
                .bookmarks(ImmutableList.copyOf(bookmarks))
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
    public abstract ImmutableList<Bookmark> bookmarks();

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
     * @return the global position. It sums up the duration of all elapsed chapters plus the position
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
     * @return the author of the book or null if not set
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
    public abstract ImmutableList<Chapter> chapters();

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

    @AutoParcel.Validate
    public void validate() {
        List<Chapter> chapters = chapters();
        List<File> chapterFiles = new ArrayList<>(chapters.size());
        for (Chapter c : chapters) {
            chapterFiles.add(c.file());
        }
        Preconditions.checkArgument(playbackSpeed() >= SPEED_MIN && playbackSpeed() <= SPEED_MAX);
        for (Bookmark b : bookmarks()) {
            Preconditions.checkArgument(chapterFiles.contains(b.mediaFile()));
        }
        Preconditions.checkArgument(!chapters.isEmpty());
        Preconditions.checkArgument(chapterFiles.contains(currentFile()));
        Preconditions.checkArgument(!name().isEmpty());
        Preconditions.checkArgument(!root().isEmpty());
    }


    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }

    @AutoParcel.Builder
    public interface Builder {
        Builder currentFile(File currentFile);

        Book build();

        Builder useCoverReplacement(boolean useCoverReplacement);

        Builder name(String name);

        Builder bookmarks(ImmutableList<Bookmark> bookmarks);

        Builder chapters(ImmutableList<Chapter> chapters);

        Builder root(String root);

        Builder type(Type type);

        Builder playbackSpeed(float playbackSpeed);

        Builder id(long id);

        Builder time(int time);

        Builder author(String author);
    }
}

