package de.ph1b.audiobook.model;

import android.support.annotation.NonNull;

import java.io.File;

import auto.parcel.AutoParcel;


@AutoParcel
public abstract class Chapter implements Comparable<Chapter> {

    /**
     * Package private constructor for auto-parcel
     */
    Chapter() {
    }


    public static Chapter of(@NonNull File file,
                             @NonNull String name,
                             int duration) {
        return new AutoParcel_Chapter(name, duration, file);
    }

    @NonNull
    public abstract String name();

    public abstract int duration();

    @NonNull
    public abstract File file();

    @Override
    public int compareTo(@NonNull Chapter another) {
        return NaturalOrderComparator.FILE_COMPARATOR.compare(file(), another.file());
    }
}
