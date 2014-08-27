package de.ph1b.audiobook.utils;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BookDetail implements Parcelable{

    private int id;
    private String name;
    private String cover;
    private String thumb;
    private int[] mediaIds;
    private int position;

    public BookDetail(){

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

    public int[] getMediaIds() {
        return mediaIds;
    }

    public String getMediaIdsAsString() {
        return idsToString(mediaIds);
    }

    private String idsToString(int[] intId) {
        String idsAsString = "";
        for (int i : intId) {
            idsAsString += String.valueOf(i) + ",";
        }
        return idsAsString;
    }


    public void setMediaIDs(String mediaIDsAsString) {
        if (!mediaIDsAsString.equals("")) {
            String[] mediaIDsAsSplittedString = mediaIDsAsString.split(",");
            int[] mediaIDsAsSplittedInt = new int[mediaIDsAsSplittedString.length];
            for (int i = 0; i < mediaIDsAsSplittedInt.length; i++) {
                mediaIDsAsSplittedInt[i] = Integer.parseInt(mediaIDsAsSplittedString[i]);
            }
            this.mediaIds = mediaIDsAsSplittedInt;
        }
    }

    public void setMediaIDs(LinkedHashMap<Integer, MediaDetail> media){
        if (media.size() > 0){
            ArrayList<Integer> keyArrayList = new ArrayList<Integer>();
            for (int key : media.keySet()){
                keyArrayList.add(key);
            }
            int [] mediaIds = new int[keyArrayList.size()];
            for (int i=0; i< mediaIds.length; i++){
                mediaIds[i] = keyArrayList.get(i);
            }
            this.mediaIds = mediaIds;
        }
    }

    public int getPosition() {
        return position == 0 ? mediaIds[0] : position;
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
        destination.writeIntArray(mediaIds);
        destination.writeInt(position);
    }

    private BookDetail(Parcel pc){
        id = pc.readInt();
        name = pc.readString();
        cover = pc.readString();
        thumb = pc.readString();
        mediaIds = pc.createIntArray();
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
