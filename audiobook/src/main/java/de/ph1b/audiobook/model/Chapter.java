package de.ph1b.audiobook.model;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import net.jcip.annotations.Immutable;

import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class Chapter {

    private static final String TAG = Chapter.class.getSimpleName();
    @NonNull
    private final File file;
    @NonNull
    private final String name;
    private final int duration;


    public Chapter(@NonNull File file,
                   @NonNull String name,
                   int duration) {
        checkArgument(!name.isEmpty());

        this.file = file;
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
            return this.file.equals(that.file) && this.duration == that.duration;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + file.hashCode();
        result = PRIME * result + duration;
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "file=" + file +
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
    public File getFile() {
        return file;
    }

    public ContentValues getContentValues(long bookId) {
        ContentValues chapterCv = new ContentValues();
        chapterCv.put(DataBaseHelper.CHAPTER_DURATION, duration);
        chapterCv.put(DataBaseHelper.CHAPTER_NAME, name);
        chapterCv.put(DataBaseHelper.CHAPTER_PATH, file.getAbsolutePath());
        chapterCv.put(DataBaseHelper.BOOK_ID, bookId);
        return chapterCv;
    }
}
