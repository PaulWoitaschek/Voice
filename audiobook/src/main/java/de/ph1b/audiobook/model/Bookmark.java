package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import com.google.common.base.Objects;

import net.jcip.annotations.Immutable;

import java.io.File;


@Immutable
public class Bookmark implements Comparable<Bookmark> {

    private final int time;
    @NonNull
    private final File mediaFile;
    @NonNull
    private final String title;

    public Bookmark(@NonNull File mediaFile, @NonNull String title, int time) {
        this.mediaFile = mediaFile;
        this.title = title;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return Objects.equal(time, bookmark.time) &&
                Objects.equal(mediaFile, bookmark.mediaFile) &&
                Objects.equal(title, bookmark.title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(time, mediaFile, title);
    }

    @Override
    public String toString() {
        return "Bookmark{" +
                "time=" + time +
                ", mediaFile=" + mediaFile +
                ", title='" + title + '\'' +
                '}';
    }

    @NonNull
    public File getMediaFile() {
        return mediaFile;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public int getTime() {
        return time;
    }

    @Override
    public int compareTo(@NonNull Bookmark another) {
        // compare files
        int fileCompare = NaturalOrderComparator.FILE_COMPARATOR.compare(mediaFile, another.mediaFile);
        if (fileCompare != 0) {
            return fileCompare;
        }

        // if files are the same compare time
        if (time > another.time) {
            return 1;
        } else if (time < another.time) {
            return -1;
        }

        // if time is the same compare the titles
        return NaturalOrderComparator.naturalCompare(title, another.title);
    }
}
