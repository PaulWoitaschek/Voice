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

    public Book() {

    }

    private Book(Parcel pc) {
        id = pc.readLong();
        name = pc.readString();
        cover = pc.readString();
        time = pc.readInt();
        position = pc.readInt();
        sortId = pc.readLong();
        pc.readTypedList(containingMedia, Media.CREATOR);
    }

    @Override
    public String toString() {
        return "id:" + id + " sortId:" + sortId + " name:" + name + " cover:" + cover + " time:" + time + " position:" + position + " containingMediaSize:" + containingMedia.size();
    }

    public ArrayList<Media> getContainingMedia() {
        return containingMedia;
    }

    public void setContainingMedia(ArrayList<Media> containingMedia) {
        this.containingMedia = containingMedia;
    }

    /**
     * @param o the object to compare this instance with.
     * @return true if the objects are identically, have the same id or if an integer was set to compare,
     * if the integer interpreted as an bookId matches to the current book.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Book) {
            Book book = (Book) o;
            if (book.getId() == this.getId())
                return true;
        }
        return false;
    }

    /**
     * @return the bookId because there can be no duplicate ID because of database auto-
     * increment
     */
    @Override
    public int hashCode() {
        return (int) id;
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

    public void setId(long id) {
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
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeLong(id);
        destination.writeString(name);
        destination.writeString(cover);
        destination.writeInt(time);
        destination.writeInt(position);
        destination.writeLong(sortId);
        destination.writeTypedList(containingMedia);
    }
}
