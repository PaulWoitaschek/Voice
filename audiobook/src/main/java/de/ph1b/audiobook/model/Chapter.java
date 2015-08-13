package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import net.jcip.annotations.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class Chapter {

    private static final String TAG = Chapter.class.getSimpleName();
    @NonNull
    private final String path;
    @NonNull
    private final String name;
    private final int duration;


    public Chapter(@NonNull String path,
                   @NonNull String name,
                   int duration) {
        checkArgument(!path.isEmpty());
        checkArgument(!name.isEmpty());

        this.path = path;
        this.name = name;
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof Chapter) {
            Chapter that = (Chapter) o;
            return this.path.equals(that.path) && this.duration == that.duration;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + path.hashCode();
        result = PRIME * result + duration;
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "path=" + path +
                ",name=" + name +
                ",duration=" + duration +
                "]";
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public ContentValues getContentValues(long bookId) {
        ContentValues chapterCv = new ContentValues();
        chapterCv.put(DataBaseHelper.CHAPTER_DURATION, duration);
        chapterCv.put(DataBaseHelper.CHAPTER_NAME, name);
        chapterCv.put(DataBaseHelper.CHAPTER_PATH, path);
        chapterCv.put(DataBaseHelper.BOOK_ID, bookId);
        return chapterCv;
    }
}
