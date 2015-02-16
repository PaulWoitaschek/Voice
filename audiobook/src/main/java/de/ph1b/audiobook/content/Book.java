package de.ph1b.audiobook.content;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;


public class Book implements Parcelable {

    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel source) {
            return new Book(source);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[0];
        }
    };
    private long id;
    private long sortId;
    private String name;
    private String cover;
    private int time;
    private int position = 0;
    private ArrayList<Media> containingMedia = new ArrayList<>();
    private float playbackSpeed = 1;

    public Book(String name, String cover, ArrayList<Media> containingMedia) {
        this.name = name;
        this.cover = cover;
        this.containingMedia = containingMedia;
    }

    public Book(String name, String cover, ArrayList<Media> containingMedia, int position, int time, long sortId, long id, float playbackSpeed) {
        this(name, cover, containingMedia);
        this.position = position;
        this.time = time;
        this.sortId = sortId;
        this.id = id;
        this.playbackSpeed = playbackSpeed;
    }

    private Book(Parcel in) {
        id = in.readLong();
        name = in.readString();
        cover = in.readString();
        time = in.readInt();
        position = in.readInt();
        sortId = in.readLong();
        in.readTypedList(containingMedia, Media.CREATOR);
        playbackSpeed = in.readFloat();
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    @Override
    public String toString() {
        return "id:" + id + " sortId:" + sortId + " name:" + name + " cover:" + cover + " time:" + time + " position:" + position + " containingMediaSize:" + containingMedia.size() + ", playbackSpeed=" + playbackSpeed;
    }

    public ArrayList<Media> getContainingMedia() {
        return containingMedia;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Book) {
            Book that = (Book) o;
            return this.id == that.id;
        }

        return false;
    }

    /**
     * @return the bookId because there can be no duplicate ID because of database auto-
     * increment
     */
    @Override
    public int hashCode() {
        return (int) (31 * id);
    }


    public long getSortId() {
        return sortId;
    }

    public void setSortId(long sortId) {
        this.sortId = sortId;
    }

    public long getId() {
        return id;
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

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
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
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeString(cover);
        out.writeInt(time);
        out.writeInt(position);
        out.writeLong(sortId);
        out.writeTypedList(containingMedia);
        out.writeFloat(playbackSpeed);
    }
}
