package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import java.io.File;

import auto.parcel.AutoParcel;


@AutoParcel
public abstract class Bookmark implements Comparable<Bookmark> {

    /**
     * Package private constructor for auto-parcel
     */
    Bookmark() {
    }

    public static Bookmark of(@NonNull File mediaFile, @NonNull String title, int time) {
        return new AutoParcel_Bookmark(time, mediaFile, title);
    }

    public abstract int time();

    @NonNull
    public abstract File mediaFile();

    @NonNull
    public abstract String title();

    @Override
    public int compareTo(@NonNull Bookmark another) {
        // compare files
        int fileCompare = NaturalOrderComparator.FILE_COMPARATOR.compare(mediaFile(), another.mediaFile());
        if (fileCompare != 0) {
            return fileCompare;
        }

        // if files are the same compare time
        if (time() > another.time()) {
            return 1;
        } else if (time() < another.time()) {
            return -1;
        }

        // if time is the same compare the titles
        return NaturalOrderComparator.naturalCompare(title(), another.title());
    }
}
