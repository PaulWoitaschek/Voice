package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import net.jcip.annotations.Immutable;

import java.io.File;

import de.ph1b.audiobook.utils.Validate;

@Immutable
public class Chapter {

    private static final String TAG = Chapter.class.getSimpleName();
    @NonNull
    private final String path;
    private final int duration;

    public Chapter(Chapter that) {
        this.path = that.path;
        this.duration = that.duration;
    }

    public Chapter(@NonNull String path,
                   int duration) {

        new Validate().notNull(path)
                .notEmpty(path);
        this.path = path;
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
                ",duration=" + duration +
                "]";
    }

    @NonNull
    public String getName() {
        // checking for dot index because otherwise a file called ".mp3" would have no name.
        String fileName = new File(path).getName();
        int dotIndex = fileName.indexOf(".");
        String chapterName;
        if (dotIndex > 0) {
            chapterName = fileName.substring(0, dotIndex);
        } else {
            chapterName = fileName;
        }
        return chapterName;
    }

    public int getDuration() {
        return duration;
    }

    @NonNull
    public String getPath() {
        return path;
    }
}
