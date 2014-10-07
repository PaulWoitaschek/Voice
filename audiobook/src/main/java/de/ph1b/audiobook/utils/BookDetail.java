package de.ph1b.audiobook.utils;


import android.os.Parcel;
import android.os.Parcelable;


public class BookDetail implements Parcelable {

    private int id;
    private String name;
    private String cover;
    private String thumb;
    private int position;

    public BookDetail() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(id);
        destination.writeString(name);
        destination.writeString(cover);
        destination.writeString(thumb);
        destination.writeInt(position);
    }

    private BookDetail(Parcel pc) {
        id = pc.readInt();
        name = pc.readString();
        cover = pc.readString();
        thumb = pc.readString();
        position = pc.readInt();
    }

    public static final Parcelable.Creator<BookDetail> CREATOR = new Parcelable.Creator<BookDetail>() {
        @Override
        public BookDetail createFromParcel(Parcel source) {
            return new BookDetail(source);
        }

        @Override
        public BookDetail[] newArray(int size) {
            return new BookDetail[0];
        }
    };
}
