package de.ph1b.audiobook.content;


import android.os.Parcel;
import android.os.Parcelable;


public class Media implements Parcelable {

    public static final Parcelable.Creator<Media> CREATOR = new Parcelable.Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel in) {
            return new Media(in);
        }


        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }

    };
    private final String path;
    private final String name;
    private final long bookId;
    private long id;
    private int duration;


    public Media(String path, String name, long bookId) {
        this.path = path;
        this.name = name;
        this.bookId = bookId;
    }

    private Media(Parcel pc) {
        path = pc.readString();
        name = pc.readString();
        id = pc.readLong();
        duration = pc.readInt();
        bookId = pc.readLong();
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
        else if (o instanceof Media) {
            Media m = (Media) o;
            if (m.id == this.id)
                return true;
        }
        return false;
    }

    /**
     * @return the mediaId because they are unique because of database-autoincrement
     */
    @Override
    public int hashCode() {
        return (int) id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getBookId() {
        return bookId;
    }

    public String getName() {
        return name;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId(){
        return id;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
        out.writeString(name);
        out.writeLong(id);
        out.writeInt(duration);
        out.writeLong(bookId);
    }
}