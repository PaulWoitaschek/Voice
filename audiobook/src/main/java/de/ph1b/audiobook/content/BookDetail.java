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
    private long id;
    private long sortId;
    private String name;
    private String cover;
    private long currentMediaId;
    private int currentMediaPosition;


    public BookDetail() {

    }

    private BookDetail(Parcel pc) {
        id = pc.readLong();
        name = pc.readString();
        cover = pc.readString();
        currentMediaId = pc.readLong();
        currentMediaPosition = pc.readInt();
        sortId = pc.readLong();
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
        return (int) id;
    }

    @Override
    public String toString() {
        return "BookId: " + id;
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

    public long getCurrentMediaId() {
        return currentMediaId;
    }

    public void setCurrentMediaId(long currentMediaId) {
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
        destination.writeLong(id);
        destination.writeString(name);
        destination.writeString(cover);
        destination.writeLong(currentMediaId);
        destination.writeInt(currentMediaPosition);
        destination.writeLong(sortId);
    }
}
