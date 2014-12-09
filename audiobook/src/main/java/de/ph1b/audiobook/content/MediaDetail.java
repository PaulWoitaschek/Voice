package de.ph1b.audiobook.content;


import android.os.Parcel;
import android.os.Parcelable;


public class MediaDetail implements Parcelable {

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
    private String path;
    private String name;
    private int id;
    private int duration;
    private int bookId;


    public MediaDetail() {

    }

    private MediaDetail(Parcel pc) {
        path = pc.readString();
        name = pc.readString();
        id = pc.readInt();
        duration = pc.readInt();
        bookId = pc.readInt();
    }

    /**
     * @param o the object to compare this instance with.
     * @return true if its the same object or it has the same id
     */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        else if (o == this)
            return true;
        else if (o instanceof MediaDetail) {
            MediaDetail m = (MediaDetail) o;
            if (m.getId() == this.getId())
                return true;
        }
        return false;
    }

    /**
     * @return the mediaId because they are unique because of database-autoincrement
     */
    @Override
    public int hashCode() {
        return id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
        out.writeString(name);
        out.writeInt(id);
        out.writeInt(duration);
        out.writeInt(bookId);
    }
}