package de.ph1b.audiobook.utils;


import android.os.Parcel;
import android.os.Parcelable;


public class MediaDetail implements Parcelable {

    private String path;
    private String name;

    private int position = 0;
    private int id;
    private int duration;
    private int bookId;


    public MediaDetail() {

    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
        out.writeString(name);
        out.writeInt(position);
        out.writeInt(id);
        out.writeInt(duration);
        out.writeInt(bookId);
    }

    private MediaDetail(Parcel pc) {
        path = pc.readString();
        name = pc.readString();
        position = pc.readInt();
        id = pc.readInt();
        duration = pc.readInt();
        bookId = pc.readInt();
    }

    public static final Parcelable.Creator<MediaDetail> CREATOR = new Parcelable.Creator<MediaDetail>() {
        @Override
        public MediaDetail createFromParcel(Parcel in) {
            return new MediaDetail(in);
        }


        @Override
        public MediaDetail[] newArray(int size) {
            return new MediaDetail[size];
        }

    };
}