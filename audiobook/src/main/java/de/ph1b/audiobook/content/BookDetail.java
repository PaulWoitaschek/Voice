package de.ph1b.audiobook.content;


import android.os.Parcel;
import android.os.Parcelable;


public class BookDetail implements Parcelable {

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
    private int id;
    private int sortId;
    private String name;
    private String cover;
    private int currentMediaId;
    private int currentMediaPosition;


    public BookDetail() {

    }

    private BookDetail(Parcel pc) {
        id = pc.readInt();
        name = pc.readString();
        cover = pc.readString();
        currentMediaId = pc.readInt();
        currentMediaPosition = pc.readInt();
        sortId = pc.readInt();
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
        } else if (o instanceof BookDetail) {
            BookDetail book = (BookDetail) o;
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
        return id;
    }

    @Override
    public String toString() {
        return "BookId: " + id;
    }

    public int getSortId() {
        return sortId;
    }

    public void setSortId(int sortId) {
        this.sortId = sortId;
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

    public int getCurrentMediaId() {
        return currentMediaId;
    }

    public void setCurrentMediaId(int currentMediaId) {
        this.currentMediaId = currentMediaId;
    }

    public int getCurrentMediaPosition() {
        return currentMediaPosition;
    }

    public void setCurrentMediaPosition(int currentMediaPosition) {
        this.currentMediaPosition = currentMediaPosition;
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
        destination.writeInt(currentMediaId);
        destination.writeInt(currentMediaPosition);
        destination.writeInt(sortId);
    }
}
