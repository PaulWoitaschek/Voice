package de.ph1b.audiobook.adapter;

import android.annotation.TargetApi;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.ArrayList;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.interfaces.OnItemClickListener;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class MediaAdapterV14 extends MediaAdapter implements ComponentCallbacks2 {

    public MediaAdapterV14(ArrayList<Book> data, Context c, OnItemClickListener onItemClickListener, OnCoverChangedListener onCoverChangedListener) {
        super(data, c, onItemClickListener, onCoverChangedListener);
    }


    @Override
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            imageCache.evictAll();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            imageCache.trimToSize(imageCache.size() / 2);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }
}
