package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import com.google.common.base.Objects;

import java.io.File;


public class Chapter implements Comparable<Chapter> {

    @NonNull
    private final File file;
    @NonNull
    private final String name;
    private final int duration;

    public Chapter(@NonNull File file,
                   @NonNull String name,
                   int duration) {

        this.file = file;
        this.name = name;
        this.duration = duration;
    }


    @Override
    public String toString() {
        return "Chapter{" +
                "file=" + file +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chapter chapter = (Chapter) o;
        return Objects.equal(duration, chapter.duration) &&
                Objects.equal(file, chapter.file) &&
                Objects.equal(name, chapter.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file, name, duration);
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

    @Override
    public int compareTo(@NonNull Chapter another) {
        return NaturalOrderComparator.FILE_COMPARATOR.compare(file, another.file);
    }
}
